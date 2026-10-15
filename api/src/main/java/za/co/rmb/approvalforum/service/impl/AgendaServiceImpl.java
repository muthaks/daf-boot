package za.co.rmb.approvalforum.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.data.ApprovalForumAgendaRepository;
import za.co.rmb.approvalforum.data.entity.*;
import za.co.rmb.approvalforum.model.ListManagementList;
import za.co.rmb.approvalforum.model.agenda.*;
import za.co.rmb.approvalforum.service.AgendaAccess;
import za.co.rmb.approvalforum.service.AgendaCalendarService;
import za.co.rmb.approvalforum.service.AgendaMailService;
import za.co.rmb.approvalforum.service.AgendaService;
import za.co.rmb.approvalforum.service.InvitationStyle;
import za.co.rmb.approvalforum.service.ApprovalForumListManagementService;

import jakarta.inject.Inject;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AgendaServiceImpl implements AgendaService {

    private static final Logger logger = LoggerFactory.getLogger(AgendaServiceImpl.class);

    // Item lifecycle (BRS section 6.3): Captured -> Confirmed (mandatory papers in) -> Submitted
    // -> Returned (back to the analyst) / Approved (on the agenda) / Rejected. Removed = dropped.
    static final String STATUS_CAPTURED = "Captured";
    static final String STATUS_CONFIRMED = "Confirmed";
    static final String STATUS_SUBMITTED = "Submitted";
    static final String STATUS_RETURNED = "Returned";
    static final String STATUS_APPROVED = "Approved";
    static final String STATUS_REJECTED = "Rejected";
    static final String STATUS_REMOVED = "Removed";

    static final String DOC_TYPE_CREDIT_PAPER = "Credit Paper";

    static final String DOC_PENDING = "Pending";
    static final String DOC_ACCEPTED = "Accepted";
    static final String DOC_REJECTED = "Rejected";
    static final String DOC_SUPERSEDED = "Superseded";

    // Meeting lifecycle (BRS section 6.2): Draft -> Compiled -> Locked -> Held -> Closed, or
    // Cancelled from any pre-Held state. Re-open takes Locked back to Draft; a Closed record is
    // only re-opened through an audited reviewer override (RULE-015).
    static final String MEETING_DRAFT = "Draft";
    static final String MEETING_COMPILED = "Compiled";
    static final String MEETING_LOCKED = "Locked";
    static final String MEETING_HELD = "Held";
    static final String MEETING_CLOSED = "Closed";
    static final String MEETING_CANCELLED = "Cancelled";

    // Workflow action names recorded in AGW (BRS section 6.6).
    static final String ACTION_SUBMIT = "Submit";
    static final String ACTION_RETURN = "Return";
    static final String ACTION_APPROVE = "Approve";
    static final String ACTION_REJECT = "Reject";
    static final String ACTION_REOPEN = "Reopen";
    static final String ACTION_REMOVE = "Remove";
    static final String ACTION_LOCK = "Lock";
    static final String ACTION_UNLOCK = "Unlock";
    static final String ACTION_HELD = "Held";
    static final String ACTION_CANCEL = "Cancel";
    static final String ACTION_CLOSE = "Close";

    static final String CONDITION_OPEN = "Open";
    static final String CONDITION_CLOSED = "Closed";
    static final String CONDITION_WAIVED = "Waived";

    private static final int BOOKING_WEEKS_AHEAD = 3;
    private static final LocalTime CUT_OFF_TIME = LocalTime.of(17, 0);
    private static final DayOfWeek CUT_OFF_DAY = DayOfWeek.THURSDAY;
    private static final LocalTime AGENDA_START_TIME = LocalTime.of(8, 0); // default meeting start; secretary can adjust
    private static final int REMINDER_WINDOW_DAYS = 2;

    // Roster columns on AGI that stand in for participants on items captured before AGP existed,
    // keyed by the committee-role name used in AGC.QuorumRoles.
    private static final String[][] LEGACY_ROSTER_ROLES = {
            {"Chair", "chair"}, {"IndependentMember", "independentMember"},
            {"PortfolioCce1", "portfolioCce1"}, {"PortfolioCce2", "portfolioCce2"},
            {"BusinessRep1", "businessRep1"}, {"SeniorBusinessRep", "seniorBusinessRep"},
            {"LecRep", "lecRep"}, {"EqMemberFrmExec", "eqMemberFrmExec"}, {"Scribe", "scribe"}};

    private final ApprovalForumAgendaRepository agendaRepository;
    private final AgendaMailService agendaMailService;
    private final ApprovalForumListManagementService listManagementService;
    private final AgendaCalendarService calendarService;

    @Inject
    AgendaServiceImpl(ApprovalForumAgendaRepository agendaRepository, AgendaMailService agendaMailService,
                      ApprovalForumListManagementService listManagementService, AgendaCalendarService calendarService) {
        this.agendaRepository = agendaRepository;
        this.agendaMailService = agendaMailService;
        this.listManagementService = listManagementService;
        this.calendarService = calendarService;
    }

    @Override
    public List<AgendaCommitteeDTO> getCommittees() {
        return agendaRepository.getCommittees().stream().map(this::toCommitteeDTO).collect(Collectors.toList());
    }

    @Override
    public AgendaCommitteeDTO getCommittee(int agcId) {
        ApprovalForumAgcAgendaCommitteeEntity entity = agendaRepository.getCommittee(agcId);
        return entity == null ? null : toCommitteeDTO(entity);
    }

    @Override
    public AgendaCommitteeDTO updateCommittee(AgendaCommitteeDTO committee, String user) {
        ApprovalForumAgcAgendaCommitteeEntity entity = agendaRepository.getCommittee(committee.getAgcId());
        if (entity == null) {
            throw new IllegalArgumentException("Unknown committee: " + committee.getAgcId());
        }
        entity.setQuorumSize(committee.getQuorumSize());
        entity.setMeetingDay(committee.getMeetingDay());
        entity.setChair(committee.getChair());
        entity.setChairId(committee.getChairId());
        entity.setAltChair(committee.getAltChair());
        entity.setAltChairId(committee.getAltChairId());
        entity.setMeetingSecretary(committee.getMeetingSecretary());
        entity.setMeetingSecretaryId(committee.getMeetingSecretaryId());
        entity.setMinuteTaker(committee.getMinuteTaker());
        entity.setMinuteTakerId(committee.getMinuteTakerId());
        entity.setAdministrator(committee.getAdministrator());
        entity.setAdministratorId(committee.getAdministratorId());
        entity.setApproverComposition(committee.getApproverComposition());
        entity.setPageGuidance(committee.getPageGuidance());
        entity.setListName(committee.getListName());
        applyCommitteeConfig(entity, committee);
        return toCommitteeDTO(agendaRepository.saveCommittee(entity));
    }

    // ---- BRS alignment: committee & calendar administration (BR-001, BR-002, RULE-004, RULE-007) ----

    @Override
    public List<AgendaCommitteeDTO> getAllCommittees() {
        return agendaRepository.getAllCommittees().stream().map(this::toCommitteeDTO).collect(Collectors.toList());
    }

    @Override
    public AgendaCommitteeDTO createCommittee(AgendaCommitteeDTO committee, String user) {
        requireText(committee.getCommitteeName(), "Committee name is required");
        requireText(committee.getMandateLevel(), "Mandate level is required");
        requireText(committee.getMeetingDay(), "Meeting day is required");
        parseDay(committee.getMeetingDay());
        if (committee.getQuorumSize() <= 0) {
            throw new IllegalStateException("Quorum size must be greater than zero");
        }
        ApprovalForumAgcAgendaCommitteeEntity entity = new ApprovalForumAgcAgendaCommitteeEntity();
        entity.setCreatedBy(user);
        entity.setCreatedOn(now());
        entity.setActive(true);
        entity.setCommitteeName(committee.getCommitteeName().trim());
        entity.setMandateLevel(committee.getMandateLevel().trim());
        entity.setLevelDescription(committee.getLevelDescription());
        entity.setQuorumSize(committee.getQuorumSize());
        entity.setMeetingDay(committee.getMeetingDay());
        entity.setChair(committee.getChair());
        entity.setChairId(committee.getChairId());
        entity.setAltChair(committee.getAltChair());
        entity.setAltChairId(committee.getAltChairId());
        entity.setMeetingSecretary(committee.getMeetingSecretary());
        entity.setMeetingSecretaryId(committee.getMeetingSecretaryId());
        entity.setMinuteTaker(committee.getMinuteTaker());
        entity.setMinuteTakerId(committee.getMinuteTakerId());
        entity.setAdministrator(committee.getAdministrator());
        entity.setAdministratorId(committee.getAdministratorId());
        entity.setApproverComposition(committee.getApproverComposition());
        entity.setPageGuidance(committee.getPageGuidance());
        entity.setListName(committee.getListName());
        applyCommitteeConfig(entity, committee);
        if (entity.getCutOffDayOfWeek() == null) {
            entity.setCutOffDayOfWeek("Thursday");
        }
        if (entity.getCutOffTime() == null) {
            entity.setCutOffTime("17:00");
        }
        if (entity.getRequiredDocTypes() == null) {
            entity.setRequiredDocTypes(DOC_TYPE_CREDIT_PAPER);
        }
        return toCommitteeDTO(agendaRepository.saveCommittee(entity));
    }

    @Override
    public AgendaCommitteeDTO deactivateCommittee(int agcId, String user) {
        ApprovalForumAgcAgendaCommitteeEntity entity = agendaRepository.getCommittee(agcId);
        if (entity == null) {
            throw new IllegalArgumentException("Unknown committee: " + agcId);
        }
        entity.setActive(false);
        if (entity.getEffectiveTo() == null) {
            entity.setEffectiveTo(now());
        }
        return toCommitteeDTO(agendaRepository.saveCommittee(entity));
    }

    private void applyCommitteeConfig(ApprovalForumAgcAgendaCommitteeEntity entity, AgendaCommitteeDTO committee) {
        entity.setEffectiveFrom(committee.getEffectiveFrom());
        entity.setEffectiveTo(committee.getEffectiveTo());
        entity.setDefaultHeldVia(committee.getDefaultHeldVia());
        entity.setDefaultDurationMinutes(committee.getDefaultDurationMinutes());
        if (committee.getCutOffDayOfWeek() != null && !committee.getCutOffDayOfWeek().trim().isEmpty()) {
            parseDay(committee.getCutOffDayOfWeek());
            entity.setCutOffDayOfWeek(committee.getCutOffDayOfWeek().trim());
        }
        if (committee.getCutOffTime() != null && !committee.getCutOffTime().trim().isEmpty()) {
            if (toSlotMinutes(committee.getCutOffTime()) == null) {
                throw new IllegalStateException("Cut-off time must be HH:mm, got " + committee.getCutOffTime());
            }
            entity.setCutOffTime(committee.getCutOffTime().trim());
        }
        if (committee.getRequiredDocTypes() != null) {
            entity.setRequiredDocTypes(committee.getRequiredDocTypes());
        }
        if (committee.getQuorumRoles() != null) {
            entity.setQuorumRoles(committee.getQuorumRoles());
        }
    }

    /** Meetings are never booked, created or moved into the past; today is allowed. */
    private void assertNotInPast(LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalStateException("A meeting cannot be in the past (" + date + ") - choose today or a later date");
        }
    }

    /** A committee may only host meetings inside its effective window (RULE-007). */
    private boolean isEffectiveOn(ApprovalForumAgcAgendaCommitteeEntity committee, LocalDate date) {
        if (!committee.isActive()) {
            return false;
        }
        if (committee.getEffectiveFrom() != null && date.isBefore(committee.getEffectiveFrom().toLocalDateTime().toLocalDate())) {
            return false;
        }
        return committee.getEffectiveTo() == null || !date.isAfter(committee.getEffectiveTo().toLocalDateTime().toLocalDate());
    }

    @Override
    public AgendaMeetingDTO createMeeting(AgendaMeetingDTO dto, String user) {
        ApprovalForumAgcAgendaCommitteeEntity committee = agendaRepository.getCommittee(dto.getAgcId());
        if (committee == null) {
            throw new IllegalArgumentException("Unknown committee: " + dto.getAgcId());
        }
        if (dto.getMeetingDate() == null) {
            throw new IllegalStateException("Meeting date is required");
        }
        Timestamp date = Timestamp.valueOf(dto.getMeetingDate().toLocalDateTime().toLocalDate().atStartOfDay());
        assertNotInPast(date.toLocalDateTime().toLocalDate());
        if (!isEffectiveOn(committee, date.toLocalDateTime().toLocalDate())) {
            throw new IllegalStateException("Committee " + committee.getCommitteeName() + " " + committee.getMandateLevel()
                    + " is not effective on " + date.toLocalDateTime().toLocalDate());
        }
        if (agendaRepository.findMeeting(committee.getAgcId(), date) != null) {
            throw new IllegalStateException("A meeting already exists for this committee on " + date.toLocalDateTime().toLocalDate());
        }
        assertVenueForMode(dto.getHeldVia() == null ? committee.getDefaultHeldVia() : dto.getHeldVia(), dto.getVenue());
        ApprovalForumAgmAgendaMeetingEntity meeting = new ApprovalForumAgmAgendaMeetingEntity();
        meeting.setAgcAgendaCommitteeByAgcId(committee);
        meeting.setMeetingDate(date);
        meeting.setHeldVia(dto.getHeldVia() == null ? committee.getDefaultHeldVia() : dto.getHeldVia());
        meeting.setStatus(MEETING_DRAFT);
        meeting.setCutOffOn(dto.getCutOffOn() != null ? dto.getCutOffOn() : cutOffFor(committee, date));
        meeting.setStartTime(dto.getStartTime() == null ? formatTime(AGENDA_START_TIME) : dto.getStartTime());
        meeting.setEndTime(dto.getEndTime() != null ? dto.getEndTime()
                : defaultEndTime(meeting.getStartTime(), committee.getDefaultDurationMinutes()));
        meeting.setVenue(dto.getVenue());
        meeting.setTeamsJoinUrl(dto.getTeamsJoinUrl());
        meeting.setCreatedBy(user);
        meeting.setCreatedOn(now());
        meeting.setActive(true);
        return toMeetingDTO(agendaRepository.saveMeeting(meeting), false);
    }

    @Override
    public AgendaMeetingDTO updateMeeting(AgendaMeetingDTO dto, String user) {
        ApprovalForumAgmAgendaMeetingEntity meeting = agendaRepository.getMeeting(dto.getAgmId());
        if (meeting == null) {
            throw new IllegalArgumentException("Unknown meeting: " + dto.getAgmId());
        }
        assertMeetingNotHeld(meeting);
        assertMeetingNotCancelled(meeting);
        boolean dateChanged = false;
        if (dto.getMeetingDate() != null) {
            Timestamp date = Timestamp.valueOf(dto.getMeetingDate().toLocalDateTime().toLocalDate().atStartOfDay());
            if (!date.equals(meeting.getMeetingDate())) {
                // RULE-004: the secretariat may move a meeting (public holidays and approved exceptions).
                assertNotInPast(date.toLocalDateTime().toLocalDate());
                ApprovalForumAgmAgendaMeetingEntity clash = agendaRepository.findMeeting(
                        meeting.getAgcAgendaCommitteeByAgcId().getAgcId(), date);
                if (clash != null && clash.getAgmId() != meeting.getAgmId()) {
                    throw new IllegalStateException("A meeting already exists for this committee on " + date.toLocalDateTime().toLocalDate());
                }
                meeting.setMeetingDate(date);
                meeting.setCutOffOn(cutOffFor(meeting.getAgcAgendaCommitteeByAgcId(), date));
                dateChanged = true;
            }
        }
        if (dto.getCutOffOn() != null) {
            meeting.setCutOffOn(dto.getCutOffOn());
        }
        if (dto.getHeldVia() != null) {
            meeting.setHeldVia(dto.getHeldVia());
        }
        if (dto.getStartTime() != null) {
            meeting.setStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            meeting.setEndTime(dto.getEndTime());
        }
        if (dto.getVenue() != null) {
            meeting.setVenue(dto.getVenue());
        }
        if (dto.getTeamsJoinUrl() != null) {
            meeting.setTeamsJoinUrl(dto.getTeamsJoinUrl());
        }
        assertVenueForMode(meeting.getHeldVia(), meeting.getVenue());
        meeting = agendaRepository.saveMeeting(meeting);
        // Keep the Teams/Outlook event in step once one exists (locked agendas already issued).
        if (meeting.getGraphEventId() != null) {
            syncCalendarEvent(meeting, dateChanged ? "rescheduled" : "updated");
        }
        return toMeetingDTO(meeting, true);
    }

    @Override
    public AgendaMeetingDTO cancelMeeting(int agmId, String reason, String user) {
        ApprovalForumAgmAgendaMeetingEntity meeting = agendaRepository.getMeeting(agmId);
        if (meeting == null) {
            throw new IllegalArgumentException("Unknown meeting: " + agmId);
        }
        assertMeetingNotHeld(meeting);
        assertMeetingNotCancelled(meeting);
        requireText(reason, "A cancellation reason is required");
        String from = meeting.getStatus();
        meeting.setStatus(MEETING_CANCELLED);
        meeting.setCancelledBy(user);
        meeting.setCancelledOn(now());
        meeting.setCancelReason(reason.trim());
        meeting = agendaRepository.saveMeeting(meeting);
        recordAction(null, agmId, ACTION_CANCEL, from, MEETING_CANCELLED, reason, false, null, user);
        AgendaMeetingDTO saved = toMeetingDTO(meeting, true);
        try {
            calendarService.cancelEvent(saved, reason);
        } catch (Exception ex) {
            logger.error("Calendar cancel failed for meeting {}: {}", agmId, ex.getMessage(), ex);
        }
        try {
            agendaMailService.sendMeetingCancelledMail(saved, collectAttendeeEmails(agmId), reason);
        } catch (Exception ex) {
            logger.error("Meeting cancelled mail failed for meeting {}: {}", agmId, ex.getMessage(), ex);
        }
        return saved;
    }

    @Override
    public List<AgendaMeetingDTO> getMeetingsBetween(Timestamp from, Timestamp to) {
        return agendaRepository.getMeetingsBetween(from, to).stream()
                .map(m -> toMeetingDTO(m, false)).collect(Collectors.toList());
    }

    /** Venue is required for physical or hybrid meetings (BRS section 6.2). */
    private void assertVenueForMode(String heldVia, String venue) {
        if (heldVia == null) {
            return;
        }
        String mode = heldVia.trim().toLowerCase(Locale.ENGLISH);
        if ((mode.contains("physical") || mode.contains("hybrid") || mode.contains("person"))
                && (venue == null || venue.trim().isEmpty())) {
            throw new IllegalStateException("A venue is required for a " + heldVia.trim() + " meeting");
        }
    }

    private String defaultEndTime(String startTime, Integer durationMinutes) {
        Integer start = toSlotMinutes(startTime);
        if (start == null || durationMinutes == null || durationMinutes <= 0) {
            return null;
        }
        int end = Math.min(start + durationMinutes, 23 * 60 + 59);
        return formatTime(LocalTime.of(end / 60, end % 60));
    }

    private void requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * Members of the List Management list linked to this committee (per mandate level).
     * Returns null when the committee has no list assigned, so callers can fall back to
     * the committee's free-text members.
     */
    @Override
    public ListManagementList getCommitteeMembers(int agcId) {
        ApprovalForumAgcAgendaCommitteeEntity committee = agendaRepository.getCommittee(agcId);
        if (committee == null) {
            throw new IllegalArgumentException("Unknown committee: " + agcId);
        }
        String listName = committee.getListName();
        if (listName == null || listName.trim().isEmpty()) {
            return null;
        }
        return listManagementService.getListManagementListByName(listName.trim());
    }

    @Override
    public List<Timestamp> getAvailableMeetingDates(int agcId) {
        ApprovalForumAgcAgendaCommitteeEntity committee = agendaRepository.getCommittee(agcId);
        List<Timestamp> result = new ArrayList<>();
        if (committee == null) {
            return result;
        }
        // Explicitly scheduled (secretariat) meetings first, then the regular committee day for the
        // booking horizon - both limited to the committee's effective window and to meetings that
        // have not been cancelled (RULE-004, RULE-007).
        Set<Timestamp> dates = new LinkedHashSet<>();
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusWeeks(BOOKING_WEEKS_AHEAD);
        for (ApprovalForumAgmAgendaMeetingEntity meeting : agendaRepository.getMeetingsForCommittee(agcId)) {
            LocalDate date = meeting.getMeetingDate().toLocalDateTime().toLocalDate();
            if (!date.isBefore(today) && !date.isAfter(horizon) && !MEETING_CANCELLED.equals(meeting.getStatus())
                    && !MEETING_HELD.equals(meeting.getStatus()) && !MEETING_CLOSED.equals(meeting.getStatus())) {
                dates.add(Timestamp.valueOf(date.atStartOfDay()));
            }
        }
        DayOfWeek meetingDay = parseDay(committee.getMeetingDay());
        LocalDate candidate = today.with(TemporalAdjusters.nextOrSame(meetingDay));
        for (int week = 0; week < BOOKING_WEEKS_AHEAD; week++) {
            LocalDate date = candidate.plusWeeks(week);
            ApprovalForumAgmAgendaMeetingEntity existing = agendaRepository.findMeeting(agcId, Timestamp.valueOf(date.atStartOfDay()));
            if (existing != null && MEETING_CANCELLED.equals(existing.getStatus())) {
                continue;
            }
            if (isEffectiveOn(committee, date)) {
                dates.add(Timestamp.valueOf(date.atStartOfDay()));
            }
        }
        result.addAll(dates);
        result.sort(Comparator.naturalOrder());
        return result;
    }

    @Override
    public List<AgendaMeetingDTO> getMeetingsForCommittee(int agcId) {
        return agendaRepository.getMeetingsForCommittee(agcId).stream()
                .map(m -> toMeetingDTO(m, false)).collect(Collectors.toList());
    }

    @Override
    public AgendaMeetingDTO getMeeting(int agmId) {
        ApprovalForumAgmAgendaMeetingEntity entity = agendaRepository.getMeeting(agmId);
        return entity == null ? null : toMeetingDTO(entity, true);
    }

    @Override
    public AgendaItemDTO bookItem(BookAgendaItemRequest request, String user) {
        ApprovalForumAgcAgendaCommitteeEntity committee = agendaRepository.getCommittee(request.getAgcId());
        if (committee == null) {
            throw new IllegalArgumentException("Unknown committee: " + request.getAgcId());
        }
        if (request.getMeetingDate() == null) {
            throw new IllegalStateException("Meeting date is required");
        }
        assertNotInPast(request.getMeetingDate().toLocalDateTime().toLocalDate());

        ApprovalForumAgmAgendaMeetingEntity meeting = agendaRepository.findMeeting(request.getAgcId(), request.getMeetingDate());
        assertMeetingNotHeld(meeting);
        assertMeetingNotCancelled(meeting);
        assertMeetingNotLocked(meeting);
        if (meeting == null) {
            LocalDate date = request.getMeetingDate().toLocalDateTime().toLocalDate();
            if (!isEffectiveOn(committee, date)) {
                throw new IllegalStateException("Committee " + committee.getCommitteeName() + " "
                        + committee.getMandateLevel() + " is not effective on " + date);
            }
            meeting = new ApprovalForumAgmAgendaMeetingEntity();
            meeting.setAgcAgendaCommitteeByAgcId(committee);
            meeting.setMeetingDate(request.getMeetingDate());
            meeting.setHeldVia(request.getHeldVia() == null ? committee.getDefaultHeldVia() : request.getHeldVia());
            meeting.setStatus(MEETING_DRAFT);
            meeting.setCutOffOn(cutOffFor(committee, request.getMeetingDate()));
            meeting.setStartTime(formatTime(AGENDA_START_TIME));
            meeting.setEndTime(defaultEndTime(meeting.getStartTime(), committee.getDefaultDurationMinutes()));
            meeting.setCreatedBy(user);
            meeting.setCreatedOn(now());
            meeting.setActive(true);
            meeting = agendaRepository.saveMeeting(meeting);
        }

        AgendaItemDTO dto = request.getItem();
        assertNoOverlappingSlot(meeting.getAgmId(), null, dto.getStartTime(), dto.getEndTime());
        ApprovalForumAgiAgendaItemEntity item = new ApprovalForumAgiAgendaItemEntity();
        applyItemFields(item, dto);
        applyLateSubmission(item, meeting, dto.getLateSubmissionReason());
        item.setAgmAgendaMeetingByAgmId(meeting);
        item.setItemStatus(STATUS_CAPTURED);
        item.setCreatedBy(user);
        item.setCreatedOn(now());
        item.setActive(true);
        item = agendaRepository.saveItem(item);
        if (dto.getParticipants() != null && !dto.getParticipants().isEmpty()) {
            replaceParticipants(item, dto.getParticipants(), user);
        }

        AgendaItemDTO saved = toItemDTO(item, false);
        try {
            // The committee's meeting secretary is told of every booking on its agenda.
            agendaMailService.sendItemCapturedMail(saved,
                    isBlank(committee.getMeetingSecretaryId()) ? "" : committee.getMeetingSecretaryId().trim());
        } catch (Exception ex) {
            logger.error("Item captured mail failed for item {}: {}", saved.getAgiId(), ex.getMessage(), ex);
        }
        return saved;
    }

    @Override
    public AgendaItemDTO updateItem(AgendaItemDTO dto, String user) {
        ApprovalForumAgiAgendaItemEntity item = agendaRepository.getItem(dto.getAgiId());
        if (item == null) {
            throw new IllegalArgumentException("Unknown agenda item: " + dto.getAgiId());
        }
        assertMeetingNotLocked(item.getAgmAgendaMeetingByAgmId());
        assertMeetingNotHeld(item.getAgmAgendaMeetingByAgmId());
        assertItemEditable(item);
        assertNoOverlappingSlot(item.getAgmAgendaMeetingByAgmId().getAgmId(), item.getAgiId(),
                dto.getStartTime(), dto.getEndTime());
        applyItemFields(item, dto);
        applyLateSubmission(item, item.getAgmAgendaMeetingByAgmId(), dto.getLateSubmissionReason());
        item.setLateApprovedBy(dto.getLateApprovedBy());
        item.setLateApprovedOn(dto.getLateApprovedOn());
        item.setOutOfGuidanceApprovedBy(dto.getOutOfGuidanceApprovedBy());
        item.setOutOfGuidanceApprovedOn(dto.getOutOfGuidanceApprovedOn());
        item = agendaRepository.saveItem(item);
        if (dto.getParticipants() != null) {
            replaceParticipants(item, dto.getParticipants(), user);
        }
        return toItemDTO(item, true);
    }

    /** Only the analyst-owned states may be edited; submitted/approved items go through the workflow. */
    private void assertItemEditable(ApprovalForumAgiAgendaItemEntity item) {
        String status = item.getItemStatus();
        if (STATUS_SUBMITTED.equals(status) || STATUS_APPROVED.equals(status)
                || STATUS_REJECTED.equals(status) || STATUS_REMOVED.equals(status)) {
            throw new IllegalStateException("Agenda item " + item.getAgiId() + " is " + status
                    + " - it must be returned or re-opened before it can be edited");
        }
    }

    /**
     * RULE-003: late is decided by the system against the meeting cut-off, never by the user, and
     * a late submission must carry a reason.
     */
    private void applyLateSubmission(ApprovalForumAgiAgendaItemEntity item, ApprovalForumAgmAgendaMeetingEntity meeting, String reason) {
        boolean late = meeting != null && meeting.getCutOffOn() != null && now().after(meeting.getCutOffOn());
        item.setLateSubmission(late);
        if (late) {
            if (reason == null || reason.trim().isEmpty()) {
                throw new IllegalStateException("This is a late submission (cut-off was "
                        + meeting.getCutOffOn().toLocalDateTime().toLocalDate() + " "
                        + formatTime(meeting.getCutOffOn().toLocalDateTime().toLocalTime())
                        + ") - a late submission reason is required");
            }
            item.setLateSubmissionReason(reason.trim());
        }
    }

    @Override
    public List<AgendaItemDTO> getAllItems() {
        // Post-meeting outcomes ride along so the dashboard can chart decisions.
        return agendaRepository.getAllItems().stream().map(i -> {
            AgendaItemDTO dto = toItemDTO(i, false);
            ApprovalForumAgpmAgendaPostMeetingEntity pm = agendaRepository.getPostMeetingForItem(i.getAgiId());
            if (pm != null) {
                dto.setPostMeeting(toPostMeetingDTO(pm));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<AgendaItemDTO> getItemsForUser(String userId) {
        return agendaRepository.getItemsForUser(userId).stream().map(i -> toItemDTO(i, false)).collect(Collectors.toList());
    }

    /** BR-024: an analyst's agenda list holds the items they booked, nobody else's. */
    @Override
    public List<AgendaItemDTO> getItemsCreatedBy(String userId) {
        return agendaRepository.getItemsCreatedBy(userId).stream().map(i -> {
            AgendaItemDTO dto = toItemDTO(i, false);
            ApprovalForumAgpmAgendaPostMeetingEntity pm = agendaRepository.getPostMeetingForItem(i.getAgiId());
            if (pm != null) {
                dto.setPostMeeting(toPostMeetingDTO(pm));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    // ---- Agenda item visibility (BR-024, R13) -------------------------------------------------
    // An agenda item is a confidential credit paper, so the default is no access. Exactly four
    // things open one up, and only the first grants the right to change it:
    //   owner      - the analyst who booked the item, or the primary analyst named on it
    //   reviewer   - the review analyst assigned to that item, read-only
    //   secretariat- the secretary / chair of the committee the item sits on, read-only
    //   approver   - read-only, and only once the item has been submitted for approval
    // Non-owners still act through the workflow endpoints (submit, return, approve, reject,
    // compile, lock), which carry their own role checks; what they cannot do is edit the paper.

    private static boolean sameUser(String a, String b) {
        return a != null && b != null && !a.trim().isEmpty() && a.trim().equalsIgnoreCase(b.trim());
    }

    /** The owner rule: who booked it, or who is named as the primary analyst on it. */
    private boolean isOwner(ApprovalForumAgiAgendaItemEntity item, String userId) {
        return item != null
                && (sameUser(item.getCreatedBy(), userId) || sameUser(item.getPrimaryAnalystId(), userId));
    }

    /** Submitted at least once, so an approver has a reason to look at it. */
    private boolean hasBeenSubmitted(ApprovalForumAgiAgendaItemEntity item) {
        String status = item == null ? null : item.getItemStatus();
        return STATUS_SUBMITTED.equals(status) || STATUS_APPROVED.equals(status)
                || STATUS_REJECTED.equals(status) || STATUS_RETURNED.equals(status);
    }

    /** The secretariat of the committee this item sits on: secretary, minute taker or administrator. */
    private boolean servesAsSecretary(ApprovalForumAgcAgendaCommitteeEntity c, String userId) {
        return c != null && (sameUser(c.getMeetingSecretaryId(), userId)
                || sameUser(c.getMinuteTakerId(), userId)
                || sameUser(c.getAdministratorId(), userId));
    }

    /** The chair of the committee this item sits on, or its alternate chair. */
    private boolean servesAsChair(ApprovalForumAgcAgendaCommitteeEntity c, String userId) {
        return c != null && (sameUser(c.getChairId(), userId) || sameUser(c.getAltChairId(), userId));
    }

    private boolean canView(ApprovalForumAgiAgendaItemEntity item, String userId, AgendaAccess access) {
        if (item == null) {
            return false;
        }
        if (isOwner(item, userId)) {
            return true;
        }
        AgendaAccess roles = access == null ? AgendaAccess.none() : access;
        if (roles.isReviewer() && sameUser(item.getReviewAnalystId(), userId)) {
            return true;
        }
        ApprovalForumAgcAgendaCommitteeEntity committee = committeeOf(item);
        if (roles.isSecretary() && servesAsSecretary(committee, userId)) {
            return true;
        }
        if (roles.isChairperson() && servesAsChair(committee, userId)) {
            return true;
        }
        return roles.isApprover() && hasBeenSubmitted(item);
    }

    @Override
    public List<AgendaItemDTO> getVisibleItems(String userId, AgendaAccess access) {
        return agendaRepository.getAllItems().stream()
                .filter(i -> canView(i, userId, access))
                .map(i -> toItemDTO(i, false))
                .collect(Collectors.toList());
    }

    @Override
    public boolean canViewItem(int agiId, String userId, AgendaAccess access) {
        return canView(agendaRepository.getItem(agiId), userId, access);
    }

    @Override
    public boolean canEditItem(int agiId, String userId) {
        return isOwner(agendaRepository.getItem(agiId), userId);
    }

    /**
     * The participant roster has two lives. Before the meeting is Held it is the *planned* roster
     * the analyst builds to pass the quorum check at submission, so the owner may set it. Once the
     * meeting is Held it becomes the record of who actually attended, which is the secretariat's to
     * keep - from that point only the secretary or chair of the committee may change it.
     */
    @Override
    public boolean canEditParticipants(int agiId, String userId, AgendaAccess access) {
        ApprovalForumAgiAgendaItemEntity item = agendaRepository.getItem(agiId);
        if (item == null) {
            return false;
        }
        ApprovalForumAgmAgendaMeetingEntity meeting = item.getAgmAgendaMeetingByAgmId();
        boolean held = meeting != null && MEETING_HELD.equals(meeting.getStatus());
        if (!held && isOwner(item, userId)) {
            return true;
        }
        AgendaAccess roles = access == null ? AgendaAccess.none() : access;
        ApprovalForumAgcAgendaCommitteeEntity committee = committeeOf(item);
        return (roles.isSecretary() && servesAsSecretary(committee, userId))
                || (roles.isChairperson() && servesAsChair(committee, userId));
    }

    /**
     * What happened at the meeting - decisions, attendance and conditions - is the secretariat's
     * record, not the analyst's. Only the secretary or chair of that committee may write it. The
     * owning analyst and the review analyst can read it (see {@link #canViewItem}) but never
     * change it, so the minute of a meeting cannot be edited by the person whose paper it was.
     */
    @Override
    public boolean canCaptureOutcome(int agiId, String userId, AgendaAccess access) {
        ApprovalForumAgiAgendaItemEntity item = agendaRepository.getItem(agiId);
        if (item == null) {
            return false;
        }
        AgendaAccess roles = access == null ? AgendaAccess.none() : access;
        ApprovalForumAgcAgendaCommitteeEntity committee = committeeOf(item);
        return (roles.isSecretary() && servesAsSecretary(committee, userId))
                || (roles.isChairperson() && servesAsChair(committee, userId));
    }

    /**
     * The roster (chair, analysts, quorum roles) from the committee's most recent item, so a
     * new item can be pre-filled from the previous meeting. The values are copied onto the new
     * item as an editable snapshot - not linked - so past items keep their own record.
     */
    @Override
    public AgendaItemDTO getLastRoster(int agcId) {
        ApprovalForumAgiAgendaItemEntity item = agendaRepository.getLatestItemForCommittee(agcId);
        return item == null ? null : toItemDTO(item, false);
    }

    @Override
    public AgendaItemDTO getItem(int agiId) {
        ApprovalForumAgiAgendaItemEntity item = agendaRepository.getItem(agiId);
        return item == null ? null : toItemDTO(item, true);
    }

    @Override
    public AgendaItemDTO attachDocument(int agiId, String docStashId, String fileName, String user) {
        ApprovalForumAgiAgendaItemEntity item = agendaRepository.getItem(agiId);
        if (item == null) {
            throw new IllegalArgumentException("Unknown agenda item: " + agiId);
        }
        assertMeetingNotHeld(item.getAgmAgendaMeetingByAgmId());

        ApprovalForumAgidAgendaItemDocumentEntity document = new ApprovalForumAgidAgendaItemDocumentEntity();
        document.setAgiAgendaItemByAgiId(item);
        document.setDocStashId(docStashId);
        document.setFileName(fileName);
        document.setUploadedBy(user);
        document.setUploadedOn(now());
        document.setCreatedBy(user);
        document.setCreatedOn(now());
        document.setActive(true);
        agendaRepository.saveDocument(document);

        // A booked slot is only confirmed once the credit paper is uploaded.
        item.setItemStatus(STATUS_CONFIRMED);
        AgendaItemDTO saved = toItemDTO(agendaRepository.saveItem(item), true);
        try {
            agendaMailService.sendItemConfirmedMail(saved);
        } catch (Exception ex) {
            logger.error("Item confirmed mail failed for item {}: {}", agiId, ex.getMessage(), ex);
        }
        return saved;
    }

    @Override
    public AgendaItemDTO uploadDocument(int agiId, String docType, String fileName, String fileExtension, byte[] fileData, String user) {
        ApprovalForumAgiAgendaItemEntity item = agendaRepository.getItem(agiId);
        if (item == null) {
            throw new IllegalArgumentException("Unknown agenda item: " + agiId);
        }
        assertMeetingNotLocked(item.getAgmAgendaMeetingByAgmId());
        assertMeetingNotHeld(item.getAgmAgendaMeetingByAgmId());
        if (STATUS_REJECTED.equals(item.getItemStatus()) || STATUS_REMOVED.equals(item.getItemStatus())) {
            throw new IllegalStateException("Agenda item " + agiId + " is " + item.getItemStatus() + " - documents cannot be added");
        }

        // A new upload of the same type supersedes the previous version (BRS section 6.5).
        int version = 1;
        for (ApprovalForumAgidAgendaItemDocumentEntity previous : agendaRepository.getDocumentsForItem(agiId)) {
            if (previous.getDocType() != null && previous.getDocType().equalsIgnoreCase(docType)) {
                version = Math.max(version, previous.getVersion() + 1);
                if (!DOC_SUPERSEDED.equals(previous.getValidationStatus())) {
                    previous.setValidationStatus(DOC_SUPERSEDED);
                    agendaRepository.saveDocument(previous);
                }
            }
        }

        ApprovalForumAgidAgendaItemDocumentEntity document = new ApprovalForumAgidAgendaItemDocumentEntity();
        document.setAgiAgendaItemByAgiId(item);
        document.setDocType(docType);
        document.setFileName(fileName);
        document.setFileExtension(fileExtension);
        document.setFileData(fileData);
        document.setFileSize(fileData == null ? null : (long) fileData.length);
        document.setUploadedBy(user);
        document.setUploadedOn(now());
        document.setCreatedBy(user);
        document.setCreatedOn(now());
        document.setActive(true);
        document.setMandatory(isMandatoryDocType(committeeOf(item), docType));
        document.setValidationStatus(DOC_PENDING);
        document.setVersion(version);
        agendaRepository.saveDocument(document);

        // A booked slot is only confirmed once every mandatory paper is in (RULE-002); other
        // document types attach without changing the item status.
        if (STATUS_CAPTURED.equals(item.getItemStatus()) && mandatoryDocumentsPresent(item)) {
            item.setItemStatus(STATUS_CONFIRMED);
            AgendaItemDTO saved = toItemDTO(agendaRepository.saveItem(item), true);
            try {
                agendaMailService.sendItemConfirmedMail(saved);
            } catch (Exception ex) {
                logger.error("Item confirmed mail failed for item {}: {}", agiId, ex.getMessage(), ex);
            }
            return saved;
        }

        return toItemDTO(item, true);
    }

    @Override
    public AgendaItemDocumentDTO validateDocument(int agidId, String status, String comment, String user) {
        ApprovalForumAgidAgendaItemDocumentEntity document = agendaRepository.getDocument(agidId);
        if (document == null) {
            throw new IllegalArgumentException("Unknown document: " + agidId);
        }
        if (!DOC_ACCEPTED.equals(status) && !DOC_REJECTED.equals(status) && !DOC_PENDING.equals(status)) {
            throw new IllegalStateException("Validation status must be Pending, Accepted or Rejected");
        }
        if (DOC_REJECTED.equals(status)) {
            requireText(comment, "A comment is required when rejecting a document");
        }
        document.setValidationStatus(status);
        document.setValidatedBy(user);
        document.setValidatedOn(now());
        document.setValidationComment(comment);
        return toDocumentDTO(agendaRepository.saveDocument(document));
    }

    private ApprovalForumAgcAgendaCommitteeEntity committeeOf(ApprovalForumAgiAgendaItemEntity item) {
        if (item == null || item.getAgmAgendaMeetingByAgmId() == null) {
            return null;
        }
        return item.getAgmAgendaMeetingByAgmId().getAgcAgendaCommitteeByAgcId();
    }

    private List<String> requiredDocTypes(ApprovalForumAgcAgendaCommitteeEntity committee) {
        String configured = committee == null ? null : committee.getRequiredDocTypes();
        if (configured == null || configured.trim().isEmpty()) {
            configured = DOC_TYPE_CREDIT_PAPER;
        }
        return Arrays.stream(configured.split(";")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    private boolean isMandatoryDocType(ApprovalForumAgcAgendaCommitteeEntity committee, String docType) {
        return docType != null && requiredDocTypes(committee).stream().anyMatch(t -> t.equalsIgnoreCase(docType.trim()));
    }

    /** RULE-002: every configured mandatory paper has a current (not superseded/rejected) upload. */
    private boolean mandatoryDocumentsPresent(ApprovalForumAgiAgendaItemEntity item) {
        List<ApprovalForumAgidAgendaItemDocumentEntity> documents = agendaRepository.getDocumentsForItem(item.getAgiId());
        for (String required : requiredDocTypes(committeeOf(item))) {
            boolean present = documents.stream().anyMatch(d -> d.getDocType() != null
                    && d.getDocType().equalsIgnoreCase(required)
                    && !DOC_SUPERSEDED.equals(d.getValidationStatus())
                    && !DOC_REJECTED.equals(d.getValidationStatus()));
            if (!present) {
                return false;
            }
        }
        return true;
    }

    private List<String> missingMandatoryDocuments(ApprovalForumAgiAgendaItemEntity item) {
        List<ApprovalForumAgidAgendaItemDocumentEntity> documents = agendaRepository.getDocumentsForItem(item.getAgiId());
        List<String> missing = new ArrayList<>();
        for (String required : requiredDocTypes(committeeOf(item))) {
            boolean present = documents.stream().anyMatch(d -> d.getDocType() != null
                    && d.getDocType().equalsIgnoreCase(required)
                    && !DOC_SUPERSEDED.equals(d.getValidationStatus())
                    && !DOC_REJECTED.equals(d.getValidationStatus()));
            if (!present) {
                missing.add(required);
            }
        }
        return missing;
    }

    // ---- BRS alignment: review & approval workflow (BR-010, section 6.6, RULE-001, RULE-009) ----

    @Override
    public AgendaItemDTO submitItem(int agiId, String quorumOverrideReason, String user) {
        ApprovalForumAgiAgendaItemEntity item = requireItem(agiId);
        ApprovalForumAgmAgendaMeetingEntity meeting = item.getAgmAgendaMeetingByAgmId();
        assertMeetingNotLocked(meeting);
        assertMeetingNotHeld(meeting);
        assertMeetingNotCancelled(meeting);
        String from = item.getItemStatus();
        if (!STATUS_CAPTURED.equals(from) && !STATUS_CONFIRMED.equals(from) && !STATUS_RETURNED.equals(from)) {
            throw new IllegalStateException("Agenda item " + agiId + " is " + from + " and cannot be submitted");
        }
        // RULE-001: mandatory information complete.
        List<String> missingFields = new ArrayList<>();
        if (isBlank(item.getCounterpartyName())) missingFields.add("counterparty");
        if (isBlank(item.getPurposeOfSubmission())) missingFields.add("purpose of submission");
        if (isBlank(item.getBusinessUnit())) missingFields.add("business unit");
        if (isBlank(item.getPrimaryAnalyst())) missingFields.add("credit analyst");
        if (item.getEstimatedMinutes() == null || item.getEstimatedMinutes() <= 0) missingFields.add("requested duration");
        if (!missingFields.isEmpty()) {
            throw new IllegalStateException("Cannot submit - complete the mandatory fields first: " + String.join(", ", missingFields));
        }
        // RULE-002: mandatory papers uploaded.
        List<String> missingDocs = missingMandatoryDocuments(item);
        if (!missingDocs.isEmpty()) {
            throw new IllegalStateException("Cannot submit - upload the mandatory document(s) first: " + String.join(", ", missingDocs));
        }
        // RULE-003: late submissions carry a reason (recomputed at submission time).
        applyLateSubmission(item, meeting, item.getLateSubmissionReason());
        // RULE-005: role-based quorum, overridable with a recorded reason.
        AgendaQuorumResultDTO quorum = evaluateQuorum(item);
        if (!quorum.isMet()) {
            if (quorumOverrideReason == null || quorumOverrideReason.trim().isEmpty()) {
                throw new IllegalStateException("Cannot submit - quorum not met (" + quorum.getEligibleCount() + " of "
                        + quorum.getRequired() + " eligible participants" + (quorum.getMissingRoles().isEmpty() ? ""
                        : "; missing roles: " + String.join(", ", quorum.getMissingRoles())) + "). Provide a quorum override reason to proceed.");
            }
            item.setQuorumOverrideReason(quorumOverrideReason.trim());
        } else {
            item.setQuorumOverrideReason(null);
        }
        item.setItemStatus(STATUS_SUBMITTED);
        item.setSubmittedBy(user);
        item.setSubmittedOn(now());
        item = agendaRepository.saveItem(item);
        recordAction(agiId, null, ACTION_SUBMIT, from, STATUS_SUBMITTED, null,
                item.getQuorumOverrideReason() != null, item.getQuorumOverrideReason(), user);
        AgendaItemDTO saved = toItemDTO(item, true);
        try {
            agendaMailService.sendItemSubmittedMail(saved, reviewerEmails(item));
        } catch (Exception ex) {
            logger.error("Item submitted mail failed for item {}: {}", agiId, ex.getMessage(), ex);
        }
        return saved;
    }

    @Override
    public AgendaItemDTO returnItem(int agiId, String reason, String user) {
        ApprovalForumAgiAgendaItemEntity item = requireItem(agiId);
        assertMeetingNotHeld(item.getAgmAgendaMeetingByAgmId());
        requireText(reason, "A reason is required to return an agenda item");
        String from = item.getItemStatus();
        if (!STATUS_SUBMITTED.equals(from)) {
            throw new IllegalStateException("Only a submitted agenda item can be returned (item " + agiId + " is " + from + ")");
        }
        item.setItemStatus(STATUS_RETURNED);
        item = agendaRepository.saveItem(item);
        recordAction(agiId, null, ACTION_RETURN, from, STATUS_RETURNED, reason, false, null, user);
        AgendaItemDTO saved = toItemDTO(item, true);
        try {
            agendaMailService.sendItemReturnedMail(saved, reason);
        } catch (Exception ex) {
            logger.error("Item returned mail failed for item {}: {}", agiId, ex.getMessage(), ex);
        }
        return saved;
    }

    @Override
    public AgendaItemDTO approveItem(int agiId, String comments, String user) {
        ApprovalForumAgiAgendaItemEntity item = requireItem(agiId);
        assertMeetingNotHeld(item.getAgmAgendaMeetingByAgmId());
        assertMeetingNotCancelled(item.getAgmAgendaMeetingByAgmId());
        String from = item.getItemStatus();
        if (!STATUS_SUBMITTED.equals(from)) {
            throw new IllegalStateException("Only a submitted agenda item can be approved (item " + agiId + " is " + from + ")");
        }
        item.setItemStatus(STATUS_APPROVED);
        if (item.isLateSubmission() && item.getLateApprovedOn() == null) {
            item.setLateApprovedBy(user);
            item.setLateApprovedOn(now());
        }
        item = agendaRepository.saveItem(item);
        recordAction(agiId, null, ACTION_APPROVE, from, STATUS_APPROVED, comments, false, null, user);
        AgendaItemDTO saved = toItemDTO(item, true);
        try {
            agendaMailService.sendItemApprovedMail(saved);
        } catch (Exception ex) {
            logger.error("Item approved mail failed for item {}: {}", agiId, ex.getMessage(), ex);
        }
        return saved;
    }

    @Override
    public AgendaItemDTO rejectItem(int agiId, String reason, String user) {
        ApprovalForumAgiAgendaItemEntity item = requireItem(agiId);
        assertMeetingNotHeld(item.getAgmAgendaMeetingByAgmId());
        requireText(reason, "A reason is required to reject an agenda item");
        String from = item.getItemStatus();
        if (!STATUS_SUBMITTED.equals(from)) {
            throw new IllegalStateException("Only a submitted agenda item can be rejected (item " + agiId + " is " + from + ")");
        }
        item.setItemStatus(STATUS_REJECTED);
        item = agendaRepository.saveItem(item);
        recordAction(agiId, null, ACTION_REJECT, from, STATUS_REJECTED, reason, false, null, user);
        AgendaItemDTO saved = toItemDTO(item, true);
        try {
            agendaMailService.sendItemRejectedMail(saved, reason);
        } catch (Exception ex) {
            logger.error("Item rejected mail failed for item {}: {}", agiId, ex.getMessage(), ex);
        }
        return saved;
    }

    /**
     * Take an item back to the analyst. On a locked agenda this is a material change and is
     * recorded as an authorised override (RULE-009).
     */
    @Override
    public AgendaItemDTO reopenItem(int agiId, String reason, String user) {
        ApprovalForumAgiAgendaItemEntity item = requireItem(agiId);
        ApprovalForumAgmAgendaMeetingEntity meeting = item.getAgmAgendaMeetingByAgmId();
        assertMeetingNotHeld(meeting);
        assertMeetingNotCancelled(meeting);
        requireText(reason, "A reason is required to re-open an agenda item");
        String from = item.getItemStatus();
        if (!STATUS_SUBMITTED.equals(from) && !STATUS_APPROVED.equals(from) && !STATUS_REJECTED.equals(from)) {
            throw new IllegalStateException("Agenda item " + agiId + " is " + from + " and does not need re-opening");
        }
        boolean override = meeting != null && MEETING_LOCKED.equals(meeting.getStatus());
        String to = mandatoryDocumentsPresent(item) ? STATUS_CONFIRMED : STATUS_CAPTURED;
        item.setItemStatus(to);
        item = agendaRepository.saveItem(item);
        recordAction(agiId, null, ACTION_REOPEN, from, to, reason, override, override ? reason : null, user);
        return toItemDTO(item, true);
    }

    @Override
    public List<AgendaWorkflowActionDTO> getWorkflowActions(int agiId) {
        return agendaRepository.getWorkflowActionsForItem(agiId).stream().map(this::toActionDTO).collect(Collectors.toList());
    }

    private ApprovalForumAgiAgendaItemEntity requireItem(int agiId) {
        ApprovalForumAgiAgendaItemEntity item = agendaRepository.getItem(agiId);
        if (item == null) {
            throw new IllegalArgumentException("Unknown agenda item: " + agiId);
        }
        return item;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** RULE-014: every workflow action records actor, timestamp, from/to state and reason. */
    private void recordAction(Integer agiId, Integer agmId, String action, String from, String to, String comments,
                              boolean override, String overrideReason, String user) {
        ApprovalForumAgwAgendaWorkflowActionEntity entity = new ApprovalForumAgwAgendaWorkflowActionEntity();
        entity.setAgiId(agiId);
        entity.setAgmId(agmId);
        entity.setAction(action);
        entity.setFromStatus(from);
        entity.setToStatus(to);
        entity.setComments(comments);
        entity.setOverrideFlag(override);
        entity.setOverrideReason(overrideReason);
        entity.setCreatedBy(user);
        entity.setCreatedOn(now());
        entity.setActive(true);
        agendaRepository.saveWorkflowAction(entity);
    }

    /** Reviewer audience for a submission: the review analyst on the item plus the committee secretary. */
    private String reviewerEmails(ApprovalForumAgiAgendaItemEntity item) {
        Set<String> ids = new LinkedHashSet<>();
        if (!isBlank(item.getReviewAnalystId())) {
            ids.add(item.getReviewAnalystId().trim());
        }
        ApprovalForumAgcAgendaCommitteeEntity committee = committeeOf(item);
        if (committee != null && !isBlank(committee.getMeetingSecretaryId())) {
            ids.add(committee.getMeetingSecretaryId().trim());
        }
        return String.join(";", ids);
    }

    @Override
    public AgendaItemDocumentDTO getDocument(int agidId) {
        ApprovalForumAgidAgendaItemDocumentEntity entity = agendaRepository.getDocument(agidId);
        return entity == null ? null : toDocumentDTO(entity);
    }

    @Override
    public byte[] getDocumentData(int agidId) {
        ApprovalForumAgidAgendaItemDocumentEntity entity = agendaRepository.getDocument(agidId);
        return entity == null ? null : entity.getFileData();
    }

    @Override
    public AgendaItemDTO removeItem(int agiId, String user, String reason) {
        ApprovalForumAgiAgendaItemEntity item = agendaRepository.getItem(agiId);
        if (item == null) {
            throw new IllegalArgumentException("Unknown agenda item: " + agiId);
        }
        assertMeetingNotHeld(item.getAgmAgendaMeetingByAgmId());
        String from = item.getItemStatus();
        item.setItemStatus(STATUS_REMOVED);
        AgendaItemDTO saved = toItemDTO(agendaRepository.saveItem(item), false);
        recordAction(agiId, null, ACTION_REMOVE, from, STATUS_REMOVED, reason, false, null, user);
        try {
            agendaMailService.sendItemRemovedMail(saved);
        } catch (Exception ex) {
            logger.error("Item removed mail failed for item {}: {}", agiId, ex.getMessage(), ex);
        }
        return saved;
    }

    @Override
    public AgendaMeetingDTO compileAgenda(int agmId, String user) {
        ApprovalForumAgmAgendaMeetingEntity meeting = agendaRepository.getMeeting(agmId);
        if (meeting == null) {
            throw new IllegalArgumentException("Unknown meeting: " + agmId);
        }
        assertMeetingNotLocked(meeting);
        assertMeetingNotHeld(meeting);
        assertMeetingNotCancelled(meeting);

        // Items are numbered <levelDigit>.<sequence> per the spec (1.1, 1.2 under L1; 2.1 under L2).
        String level = meeting.getAgcAgendaCommitteeByAgcId().getMandateLevel();
        String group = level == null ? "1" : level.replaceAll("\\D", "");
        if (group.isEmpty()) {
            group = "1";
        }

        // Only approved items are compiled onto the agenda (BR-010); items still in review are
        // left unnumbered and reported back so the secretary can chase them.
        List<ApprovalForumAgiAgendaItemEntity> pending = agendaRepository.getItemsForMeeting(agmId).stream()
                .filter(i -> STATUS_SUBMITTED.equals(i.getItemStatus()))
                .collect(Collectors.toList());
        if (!pending.isEmpty()) {
            throw new IllegalStateException("Cannot compile - " + pending.size() + " item(s) still await review/approval: "
                    + pending.stream().map(ApprovalForumAgiAgendaItemEntity::getCounterpartyName).collect(Collectors.joining(", ")));
        }

        // Slots booked with explicit times were validated non-overlapping at capture, so they
        // are kept as reserved. Items run in slot order; unslotted items follow in capture
        // order and get proposed times back-to-back (spec: "Proposed schedule based on rules");
        // the secretary can adjust after.
        LocalTime meetingStart = meeting.getStartTime() != null && toSlotMinutes(meeting.getStartTime()) != null
                ? LocalTime.of(toSlotMinutes(meeting.getStartTime()) / 60, toSlotMinutes(meeting.getStartTime()) % 60)
                : AGENDA_START_TIME;
        List<ApprovalForumAgiAgendaItemEntity> items = agendaRepository.getItemsForMeeting(agmId).stream()
                .filter(i -> STATUS_APPROVED.equals(i.getItemStatus()))
                .sorted(Comparator
                        .comparing((ApprovalForumAgiAgendaItemEntity i) -> {
                            Integer start = toSlotMinutes(i.getStartTime());
                            return start == null ? Integer.MAX_VALUE : start;
                        })
                        .thenComparing(ApprovalForumAgiAgendaItemEntity::getCreatedOn))
                .collect(Collectors.toList());

        int sequence = 1;
        LocalTime slotStart = meetingStart;
        for (ApprovalForumAgiAgendaItemEntity item : items) {
            item.setAgendaNumber(group + "." + sequence++);
            Integer bookedStart = toSlotMinutes(item.getStartTime());
            Integer bookedEnd = toSlotMinutes(item.getEndTime());
            if (bookedStart != null && bookedEnd != null) {
                LocalTime bookedEndTime = LocalTime.of(bookedEnd / 60, bookedEnd % 60);
                if (bookedEndTime.isAfter(slotStart)) {
                    slotStart = bookedEndTime;
                }
            } else {
                int minutes = item.getEstimatedMinutes() == null ? 0 : item.getEstimatedMinutes();
                LocalTime slotEnd = slotStart.plusMinutes(minutes);
                item.setStartTime(formatTime(slotStart));
                item.setEndTime(formatTime(slotEnd));
                slotStart = slotEnd;
            }
            agendaRepository.saveItem(item);
        }

        meeting.setStatus(MEETING_COMPILED);
        meeting = agendaRepository.saveMeeting(meeting);

        // Invite the captured attendees to review the preliminary agenda (spec section 7).
        try {
            agendaMailService.sendPreliminaryAgendaMail(toMeetingDTO(meeting, false), collectAttendeeEmails(agmId));
        } catch (Exception ex) {
            logger.error("Preliminary agenda mail failed for meeting {}: {}", agmId, ex.getMessage(), ex);
        }

        return toMeetingDTO(meeting, true);
    }

    private String formatTime(LocalTime time) {
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    /**
     * Re-lay the discussion slots back-to-back from the meeting start so no two items overlap
     * (used by the "auto-adjust" action when a scheduling clash is detected). Keeps item numbers
     * and does not re-send the agenda mails - it only shifts the times.
     */
    @Override
    public AgendaMeetingDTO rescheduleAgenda(int agmId, String user) {
        ApprovalForumAgmAgendaMeetingEntity meeting = agendaRepository.getMeeting(agmId);
        if (meeting == null) {
            throw new IllegalArgumentException("Unknown meeting: " + agmId);
        }
        assertMeetingNotLocked(meeting);
        assertMeetingNotHeld(meeting);

        List<ApprovalForumAgiAgendaItemEntity> items = agendaRepository.getItemsForMeeting(agmId).stream()
                .filter(i -> STATUS_APPROVED.equals(i.getItemStatus()))
                .sorted(Comparator.comparing(ApprovalForumAgiAgendaItemEntity::getCreatedOn))
                .collect(Collectors.toList());

        Integer start = toSlotMinutes(meeting.getStartTime());
        LocalTime slotStart = start == null ? AGENDA_START_TIME : LocalTime.of(start / 60, start % 60);
        for (ApprovalForumAgiAgendaItemEntity item : items) {
            int minutes = item.getEstimatedMinutes() == null ? 0 : item.getEstimatedMinutes();
            LocalTime slotEnd = slotStart.plusMinutes(minutes);
            item.setStartTime(formatTime(slotStart));
            item.setEndTime(formatTime(slotEnd));
            slotStart = slotEnd;
            agendaRepository.saveItem(item);
        }
        return toMeetingDTO(meeting, true);
    }

    @Override
    public AgendaMeetingDTO lockMeeting(int agmId, String user) {
        ApprovalForumAgmAgendaMeetingEntity meeting = agendaRepository.getMeeting(agmId);
        if (meeting == null) {
            throw new IllegalArgumentException("Unknown meeting: " + agmId);
        }
        assertMeetingNotHeld(meeting);
        if (!MEETING_COMPILED.equals(meeting.getStatus())) {
            throw new IllegalStateException("Meeting " + agmId
                    + " must be compiled before it can be locked - compile the agenda first");
        }
        meeting.setStatus(MEETING_LOCKED);
        meeting.setLockedBy(user);
        meeting.setLockedOn(now());
        meeting = agendaRepository.saveMeeting(meeting);
        recordAction(null, agmId, ACTION_LOCK, MEETING_COMPILED, MEETING_LOCKED, null, false, null, user);

        // BR-013: create (or refresh) the Teams/Outlook meeting now that the agenda is final.
        syncCalendarEvent(meeting, "locked");
        AgendaMeetingDTO saved = toMeetingDTO(meeting, true);

        // The agenda is final - send it to all captured attendees (spec section 7).
        try {
            agendaMailService.sendFinalAgendaMail(saved, collectAttendeeEmails(agmId));
        } catch (Exception ex) {
            logger.error("Final agenda mail failed for meeting {}: {}", agmId, ex.getMessage(), ex);
        }
        return saved;
    }

    /** Create or update the calendar event for a meeting; failures are logged, never fatal. */
    private void syncCalendarEvent(ApprovalForumAgmAgendaMeetingEntity meeting, String trigger) {
        try {
            if (!calendarService.isConfigured()) {
                logger.info("Calendar integration not configured - skipping event sync ({}) for meeting {}", trigger, meeting.getAgmId());
                return;
            }
            AgendaMeetingDTO dto = toMeetingDTO(meeting, true);
            fillMeetingTimes(dto);
            List<String> attendees = new ArrayList<>();
            for (String email : collectAttendeeEmails(meeting.getAgmId()).split(";")) {
                if (!email.trim().isEmpty()) {
                    attendees.add(email.trim());
                }
            }
            List<AgendaCalendarService.Attachment> pack = new ArrayList<>();
            for (MeetingPapers.Paper paper : MeetingPapers.meetingPack(agendaRepository, dto)) {
                pack.add(new AgendaCalendarService.Attachment(paper.getFileName(), paper.getData()));
            }
            AgendaCalendarService.CalendarEvent event = calendarService.createOrUpdateEvent(dto, attendees, agendaHtml(dto, !pack.isEmpty()), pack);
            if (event != null) {
                meeting.setGraphEventId(event.getEventId());
                if (event.getJoinUrl() != null && !event.getJoinUrl().trim().isEmpty()) {
                    meeting.setTeamsJoinUrl(event.getJoinUrl());
                }
                agendaRepository.saveMeeting(meeting);
            }
        } catch (Exception ex) {
            logger.error("Calendar sync ({}) failed for meeting {}: {}", trigger, meeting.getAgmId(), ex.getMessage(), ex);
        }
    }

    /** Compact HTML agenda used as the calendar event body. */
    /**
     * A meeting without its own start or end time runs from its first approved item's start to its
     * last approved item's end, so the invitation holds the slot the agenda needs.
     */
    private void fillMeetingTimes(AgendaMeetingDTO meeting) {
        String first = null;
        String last = null;
        if (meeting.getItems() != null) {
            for (AgendaItemDTO item : meeting.getItems()) {
                if (!STATUS_APPROVED.equals(item.getItemStatus())) {
                    continue;
                }
                if (item.getStartTime() != null && (first == null || item.getStartTime().compareTo(first) < 0)) {
                    first = item.getStartTime();
                }
                if (item.getEndTime() != null && (last == null || item.getEndTime().compareTo(last) > 0)) {
                    last = item.getEndTime();
                }
            }
        }
        if (isBlank(meeting.getStartTime()) && first != null) {
            meeting.setStartTime(first);
        }
        if (isBlank(meeting.getEndTime()) && last != null) {
            meeting.setEndTime(last);
        }
    }

    /**
     * The invitation content, a short form of the booking summary in Agenda Builder: the meeting,
     * then each approved item with its deal, team and participants. Empty fields are left out.
     */
    private String agendaHtml(AgendaMeetingDTO meeting, boolean papersAttached) {
        List<AgendaItemDTO> items = new ArrayList<>();
        if (meeting.getItems() != null) {
            for (AgendaItemDTO item : meeting.getItems()) {
                if (STATUS_APPROVED.equals(item.getItemStatus())) {
                    items.add(item);
                }
            }
        }
        Map<Integer, List<AgendaParticipantDTO>> participants = new LinkedHashMap<>();
        for (AgendaItemDTO item : items) {
            participants.put(item.getAgiId(), participantsOf(item));
        }
        StringBuilder sb = new StringBuilder(InvitationStyle.details(new String[][]{
                {"Date", meeting.getMeetingDate() == null ? null
                        : new java.text.SimpleDateFormat("EEEE d MMM yyyy", Locale.ENGLISH).format(meeting.getMeetingDate())},
                {"Time", meeting.getStartTime() == null ? null
                        : meeting.getStartTime() + (meeting.getEndTime() == null ? "" : " - " + meeting.getEndTime()) + " SAST"},
                {"Held via", meeting.getHeldVia()},
                {"Venue", meeting.getVenue()},
                {"Chairperson", chairOf(meeting, items, participants)},
                {"Agenda items", items.size() + (meeting.getQuorumSize() > 0 ? " (quorum " + meeting.getQuorumSize() + ")" : "")}}));
        for (AgendaItemDTO item : items) {
            String purpose = item.getPurposeOfSubmission() == null ? null
                    : item.getPurposeOfSubmission() + (isBlank(item.getPurposeDetails()) ? "" : ": " + item.getPurposeDetails());
            List<String> people = new ArrayList<>();
            for (AgendaParticipantDTO participant : participants.get(item.getAgiId())) {
                people.add(participant.getPersonName()
                        + (isBlank(participant.getCommitteeRole()) ? "" : " (" + participant.getCommitteeRole() + ")"));
            }
            String time = timeOf(item);
            String minutes = item.getEstimatedMinutes() == null ? null : item.getEstimatedMinutes() + " mins";
            sb.append(InvitationStyle.item(
                    (item.getAgendaNumber() == null ? "" : item.getAgendaNumber() + " ")
                            + (item.getCounterpartyName() == null ? "" : item.getCounterpartyName()),
                    time == null ? minutes : time + (minutes == null ? "" : " (" + minutes + ")"),
                    InvitationStyle.details(new String[][]{
                            {"Purpose of submission", purpose},
                            {"Business unit / segment", item.getBusinessUnit()},
                            {"Rating", item.getRatingBand()},
                            {"DCP (ZAR)", item.getDcpZar() == null ? null : String.format(Locale.ENGLISH, "%,.2f", item.getDcpZar())},
                            {"Primary analyst", item.getPrimaryAnalyst()},
                            {"Review analyst", item.getReviewAnalyst()},
                            {"Credit sponsor", item.getCreditSponsor()},
                            {"BU head", item.getBuHead()},
                            {"Application type", item.getApplicationType()},
                            {"Number of pages", item.getNumberOfPages() == null ? null : String.valueOf(item.getNumberOfPages())},
                            {"Participants", people.isEmpty() ? null : String.join(", ", people), "wide"},
                            {"Comments and notes", item.getComments(), "wide"}})));
        }
        if (items.isEmpty()) {
            sb.append(InvitationStyle.note("No approved items."));
        }
        if (papersAttached) {
            sb.append(InvitationStyle.note("The credit papers for the agenda items are attached."));
        }
        return sb.toString();
    }

    /** The meeting's chair, else the chair named on its items, else the items' Chair participant. */
    private String chairOf(AgendaMeetingDTO meeting, List<AgendaItemDTO> items,
                           Map<Integer, List<AgendaParticipantDTO>> participants) {
        if (!isBlank(meeting.getChair())) {
            return meeting.getChair();
        }
        for (AgendaItemDTO item : items) {
            if (!isBlank(item.getChair())) {
                return item.getChair();
            }
        }
        for (List<AgendaParticipantDTO> list : participants.values()) {
            for (AgendaParticipantDTO participant : list) {
                if ("Chair".equalsIgnoreCase(participant.getCommitteeRole()) && !isBlank(participant.getPersonName())) {
                    return participant.getPersonName();
                }
            }
        }
        return null;
    }

    private String timeOf(AgendaItemDTO item) {
        return item.getStartTime() == null ? null
                : item.getStartTime() + (item.getEndTime() == null ? "" : " - " + item.getEndTime());
    }

    /** The item's participants as the item page lists them (meeting items are loaded without them). */
    private List<AgendaParticipantDTO> participantsOf(AgendaItemDTO item) {
        try {
            ApprovalForumAgiAgendaItemEntity entity = agendaRepository.getItem(item.getAgiId());
            if (entity != null) {
                return effectiveParticipants(entity).stream().map(this::toParticipantDTO).collect(Collectors.toList());
            }
        } catch (Exception ex) {
            logger.warn("Could not load the participants of item {}: {}", item.getAgiId(), ex.getMessage());
        }
        return new ArrayList<>();
    }

    @Override
    public AgendaMeetingDTO setMeetingStatus(int agmId, String status, String user) {
        ApprovalForumAgmAgendaMeetingEntity meeting = agendaRepository.getMeeting(agmId);
        if (meeting == null) {
            throw new IllegalArgumentException("Unknown meeting: " + agmId);
        }
        assertMeetingNotHeld(meeting);
        assertMeetingNotCancelled(meeting);
        if (MEETING_HELD.equals(status) || MEETING_CLOSED.equals(status) || MEETING_CANCELLED.equals(status)) {
            throw new IllegalStateException("Use the held / close / cancel actions to move a meeting to " + status);
        }
        String from = meeting.getStatus();
        meeting.setStatus(status);
        meeting = agendaRepository.saveMeeting(meeting);
        if (MEETING_LOCKED.equals(from) && !MEETING_LOCKED.equals(status)) {
            recordAction(null, agmId, ACTION_UNLOCK, from, status, null, true, "Agenda re-opened after lock", user);
        }
        return toMeetingDTO(meeting, true);
    }

    @Override
    public int processCutOffs() {
        int removed = 0;
        for (ApprovalForumAgmAgendaMeetingEntity meeting : agendaRepository.getMeetingsPastCutOff(now())) {
            for (ApprovalForumAgiAgendaItemEntity item : agendaRepository.getItemsForMeeting(meeting.getAgmId())) {
                if (STATUS_CAPTURED.equals(item.getItemStatus())) {
                    removeItem(item.getAgiId(), "SYSTEM", "Credit paper not uploaded by the cut-off");
                    removed++;
                }
            }
        }
        logger.info("processCutOffs() removed {} unconfirmed item(s)", removed);
        return removed;
    }

    /**
     * Reminders to upload the outstanding credit paper, sent to the analyst(s) at
     * T-2 and T-1 days before the meeting's cut-off (Agenda_Builder_Spec.md section 7).
     * Intended to run once each weekday morning.
     */
    @Override
    public int processReminders() {
        int sent = 0;
        Timestamp from = now();
        Timestamp to = Timestamp.valueOf(from.toLocalDateTime().plusDays(REMINDER_WINDOW_DAYS + 1L));
        LocalDate today = from.toLocalDateTime().toLocalDate();
        for (ApprovalForumAgmAgendaMeetingEntity meeting : agendaRepository.getMeetingsWithCutOffBetween(from, to)) {
            Timestamp cutOff = meeting.getCutOffOn();
            if (cutOff == null) {
                continue;
            }
            long daysToCutOff = ChronoUnit.DAYS.between(today, cutOff.toLocalDateTime().toLocalDate());
            if (daysToCutOff < 1 || daysToCutOff > REMINDER_WINDOW_DAYS) {
                continue;
            }
            for (ApprovalForumAgiAgendaItemEntity item : agendaRepository.getItemsForMeeting(meeting.getAgmId())) {
                if (STATUS_CAPTURED.equals(item.getItemStatus())) {
                    try {
                        agendaMailService.sendItemReminderMail(toItemDTO(item, false), cutOff, (int) daysToCutOff);
                        sent++;
                    } catch (Exception ex) {
                        logger.error("Reminder mail failed for item {}: {}", item.getAgiId(), ex.getMessage(), ex);
                    }
                }
            }
        }
        logger.info("processReminders() sent {} reminder(s)", sent);
        return sent;
    }

    @Override
    public List<AgendaNotificationDTO> getNotificationsForItem(int agiId) {
        return agendaRepository.getNotificationsForItem(agiId).stream()
                .map(this::toNotificationDTO).collect(Collectors.toList());
    }

    private AgendaNotificationDTO toNotificationDTO(ApprovalForumAgnAgendaNotificationEntity entity) {
        AgendaNotificationDTO dto = new AgendaNotificationDTO();
        dto.setAgnId(entity.getAgnId());
        dto.setAgiId(entity.getAgiId());
        dto.setAgmId(entity.getAgmId());
        dto.setNotificationType(entity.getNotificationType());
        dto.setRecipients(entity.getRecipients());
        dto.setSubject(entity.getSubject());
        dto.setStatus(entity.getStatus());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setSentOn(entity.getSentOn());
        return dto;
    }

    // Distinct attendee e-mails captured across a meeting's active items, for the invite mails.
    private String collectAttendeeEmails(int agmId) {
        Set<String> emails = new LinkedHashSet<>();
        for (ApprovalForumAgiAgendaItemEntity item : agendaRepository.getItemsForMeeting(agmId)) {
            if (STATUS_REMOVED.equals(item.getItemStatus()) || item.getAttendeeEmails() == null) {
                continue;
            }
            for (String email : item.getAttendeeEmails().split(";")) {
                if (email != null && !email.trim().isEmpty()) {
                    emails.add(email.trim());
                }
            }
        }
        return String.join(";", emails);
    }

    @Override
    public AgendaPostMeetingDTO getPostMeeting(int agiId) {
        ApprovalForumAgpmAgendaPostMeetingEntity entity = agendaRepository.getPostMeetingForItem(agiId);
        return entity == null ? null : toPostMeetingDTO(entity);
    }

    @Override
    public AgendaPostMeetingDTO savePostMeeting(AgendaPostMeetingDTO dto, String user) {
        ApprovalForumAgiAgendaItemEntity item = agendaRepository.getItem(dto.getAgiId());
        if (item == null) {
            throw new IllegalArgumentException("Unknown agenda item: " + dto.getAgiId());
        }
        assertMeetingHeld(item.getAgmAgendaMeetingByAgmId());

        ApprovalForumAgpmAgendaPostMeetingEntity entity = agendaRepository.getPostMeetingForItem(dto.getAgiId());
        if (entity != null && entity.isLocked()) {
            throw new IllegalStateException("Post-meeting record for item " + dto.getAgiId()
                    + " is locked after sign-off - a reviewer must unlock it before further edits");
        }
        if (entity == null) {
            entity = new ApprovalForumAgpmAgendaPostMeetingEntity();
            entity.setAgiAgendaItemByAgiId(item);
            entity.setCreatedBy(user);
            entity.setCreatedOn(now());
            entity.setActive(true);
        }
        entity.setActualPages(dto.getActualPages());
        entity.setActualMinutes(dto.getActualMinutes());
        entity.setDecision(dto.getDecision());
        entity.setCreditConditions(dto.getCreditConditions());
        entity.setAttendeesPresent(dto.getAttendeesPresent());
        entity.setMinuteTaker(dto.getMinuteTaker());
        entity.setScribe(dto.getScribe());
        entity.setAdministrator(dto.getAdministrator());
        entity.setAnalystSignOffBy(dto.getAnalystSignOffBy());
        entity.setAnalystSignOffOn(dto.getAnalystSignOffOn());
        entity.setChairSignOffBy(dto.getChairSignOffBy());
        entity.setChairSignOffOn(dto.getChairSignOffOn());
        entity.setDocStashOutputId(dto.getDocStashOutputId());
        entity.setActualRating(dto.getActualRating());
        entity.setActualPurpose(dto.getActualPurpose());
        entity.setActualDcp(dto.getActualDcp());
        entity.setDateOfCc(dto.getDateOfCc());
        entity.setDecisionType(dto.getDecisionType());
        entity.setDecisionDate(dto.getDecisionDate() != null ? dto.getDecisionDate()
                : (dto.getDecisionType() != null && entity.getDecisionDate() == null ? now() : entity.getDecisionDate()));

        // Chair sign-off locks the record from further edits; only a reviewer can unlock it.
        if (entity.getChairSignOffOn() != null && !entity.isLocked()) {
            entity.setLocked(true);
            entity.setLockedBy(user);
            entity.setLockedOn(now());
        }
        return toPostMeetingDTO(agendaRepository.savePostMeeting(entity));
    }

    @Override
    public AgendaMeetingDTO markMeetingHeld(int agmId, String user) {
        return markMeetingHeld(agmId, null, null, null, user);
    }

    @Override
    public AgendaMeetingDTO markMeetingHeld(int agmId, String actualStartTime, String actualEndTime, Boolean quorumAchieved, String user) {
        ApprovalForumAgmAgendaMeetingEntity meeting = agendaRepository.getMeeting(agmId);
        if (meeting == null) {
            throw new IllegalArgumentException("Unknown meeting: " + agmId);
        }
        if (!MEETING_LOCKED.equals(meeting.getStatus())) {
            throw new IllegalStateException("Meeting " + agmId
                    + " must be locked (final agenda issued) before it can be marked Held");
        }
        if (meeting.getMeetingDate() != null && meeting.getMeetingDate().after(now())) {
            throw new IllegalStateException("Meeting " + agmId + " cannot be marked Held before its meeting date");
        }
        meeting.setStatus(MEETING_HELD);
        if (actualStartTime != null) {
            meeting.setActualStartTime(actualStartTime);
        }
        if (actualEndTime != null) {
            meeting.setActualEndTime(actualEndTime);
        }
        meeting.setQuorumAchieved(quorumAchieved != null ? quorumAchieved : meetingQuorumAchieved(agmId));
        meeting = agendaRepository.saveMeeting(meeting);
        recordAction(null, agmId, ACTION_HELD, MEETING_LOCKED, MEETING_HELD,
                "Quorum achieved: " + meeting.getQuorumAchieved(), false, null, user);
        return toMeetingDTO(meeting, true);
    }

    /** Derived quorum outcome for a meeting: every approved item's roster met its committee quorum. */
    private Boolean meetingQuorumAchieved(int agmId) {
        boolean any = false;
        for (ApprovalForumAgiAgendaItemEntity item : agendaRepository.getItemsForMeeting(agmId)) {
            if (STATUS_APPROVED.equals(item.getItemStatus())) {
                any = true;
                if (!evaluateQuorum(item).isMet() && item.getQuorumOverrideReason() == null) {
                    return false;
                }
            }
        }
        return any ? Boolean.TRUE : null;
    }

    // ---- BRS alignment: close, re-open and records (BR-019, BR-020, RULE-015) ----

    @Override
    public AgendaMeetingDTO closeMeeting(int agmId, String user) {
        ApprovalForumAgmAgendaMeetingEntity meeting = agendaRepository.getMeeting(agmId);
        if (meeting == null) {
            throw new IllegalArgumentException("Unknown meeting: " + agmId);
        }
        if (!MEETING_HELD.equals(meeting.getStatus())) {
            throw new IllegalStateException("Meeting " + agmId + " must be Held before it can be closed");
        }
        List<String> outstanding = new ArrayList<>();
        for (ApprovalForumAgiAgendaItemEntity item : agendaRepository.getItemsForMeeting(agmId)) {
            if (!STATUS_APPROVED.equals(item.getItemStatus())) {
                continue;
            }
            ApprovalForumAgpmAgendaPostMeetingEntity pm = agendaRepository.getPostMeetingForItem(item.getAgiId());
            if (pm == null || pm.getChairSignOffOn() == null || isBlank(pm.getDecision()) && isBlank(pm.getDecisionType())) {
                outstanding.add(item.getCounterpartyName());
            }
        }
        if (!outstanding.isEmpty()) {
            throw new IllegalStateException("Cannot close - decision and chair sign-off outstanding for: "
                    + String.join(", ", outstanding));
        }
        meeting.setStatus(MEETING_CLOSED);
        meeting.setClosedBy(user);
        meeting.setClosedOn(now());
        meeting = agendaRepository.saveMeeting(meeting);
        recordAction(null, agmId, ACTION_CLOSE, MEETING_HELD, MEETING_CLOSED, null, false, null, user);
        AgendaMeetingDTO saved = toMeetingDTO(meeting, true);
        try {
            agendaMailService.sendMeetingClosedMail(saved, collectAttendeeEmails(agmId));
        } catch (Exception ex) {
            logger.error("Meeting closed mail failed for meeting {}: {}", agmId, ex.getMessage(), ex);
        }
        return saved;
    }

    /** RULE-015: a closed record only re-opens through an authorised, audited correction. */
    @Override
    public AgendaMeetingDTO reopenMeeting(int agmId, String reason, String user) {
        ApprovalForumAgmAgendaMeetingEntity meeting = agendaRepository.getMeeting(agmId);
        if (meeting == null) {
            throw new IllegalArgumentException("Unknown meeting: " + agmId);
        }
        if (!MEETING_CLOSED.equals(meeting.getStatus())) {
            throw new IllegalStateException("Meeting " + agmId + " is not closed");
        }
        requireText(reason, "A correction reason is required to re-open a closed meeting record");
        meeting.setStatus(MEETING_HELD);
        meeting.setClosedBy(null);
        meeting.setClosedOn(null);
        meeting = agendaRepository.saveMeeting(meeting);
        recordAction(null, agmId, ACTION_REOPEN, MEETING_CLOSED, MEETING_HELD, reason, true, reason, user);
        return toMeetingDTO(meeting, true);
    }

    @Override
    public List<AgendaMeetingRecordDTO> getMeetingRecords(int agmId) {
        return agendaRepository.getRecordsForMeeting(agmId).stream().map(this::toRecordDTO).collect(Collectors.toList());
    }

    @Override
    public AgendaMeetingRecordDTO addMeetingRecord(AgendaMeetingRecordDTO dto, String user) {
        ApprovalForumAgmAgendaMeetingEntity meeting = agendaRepository.getMeeting(dto.getAgmId());
        if (meeting == null) {
            throw new IllegalArgumentException("Unknown meeting: " + dto.getAgmId());
        }
        requireText(dto.getRecordType(), "Record type is required");
        requireText(dto.getName(), "Record name is required");
        requireText(dto.getLocation(), "Record location (URL or repository reference) is required");
        ApprovalForumAgmrAgendaMeetingRecordEntity entity = new ApprovalForumAgmrAgendaMeetingRecordEntity();
        entity.setAgmAgendaMeetingByAgmId(meeting);
        entity.setRecordType(dto.getRecordType().trim());
        entity.setName(dto.getName().trim());
        entity.setLocation(dto.getLocation().trim());
        entity.setCreatedBy(user);
        entity.setCreatedOn(now());
        entity.setActive(true);
        return toRecordDTO(agendaRepository.saveRecord(entity));
    }

    // ---- BRS alignment: participants & quorum (BR-008, BR-009, RULE-005, RULE-006) ----

    @Override
    public List<AgendaParticipantDTO> getParticipants(int agiId) {
        return effectiveParticipants(requireItem(agiId)).stream().map(this::toParticipantDTO).collect(Collectors.toList());
    }

    @Override
    public List<AgendaParticipantDTO> saveParticipants(int agiId, List<AgendaParticipantDTO> participants, String user) {
        ApprovalForumAgiAgendaItemEntity item = requireItem(agiId);
        ApprovalForumAgmAgendaMeetingEntity meeting = item.getAgmAgendaMeetingByAgmId();
        // Before the meeting the roster is editable (unless locked); after it only actual
        // attendance may be captured, and only while the record is not closed.
        boolean held = meeting != null && (MEETING_HELD.equals(meeting.getStatus()));
        if (meeting != null && MEETING_CLOSED.equals(meeting.getStatus())) {
            throw new IllegalStateException("Meeting " + meeting.getAgmId() + " is closed - participants are final");
        }
        if (held) {
            List<ApprovalForumAgpAgendaParticipantEntity> existing = effectiveParticipants(item);
            for (AgendaParticipantDTO dto : participants) {
                for (ApprovalForumAgpAgendaParticipantEntity e : existing) {
                    if (e.getAgpId() == dto.getAgpId() && e.getAgpId() != 0) {
                        e.setActualAttendance(dto.getActualAttendance());
                        agendaRepository.saveParticipant(e);
                    }
                }
            }
            return getParticipants(agiId);
        }
        assertMeetingNotLocked(meeting);
        assertMeetingNotCancelled(meeting);
        replaceParticipants(item, participants, user);
        return getParticipants(agiId);
    }

    private void replaceParticipants(ApprovalForumAgiAgendaItemEntity item, List<AgendaParticipantDTO> participants, String user) {
        for (ApprovalForumAgpAgendaParticipantEntity existing : agendaRepository.getParticipantsForItem(item.getAgiId())) {
            existing.setActive(false);
            agendaRepository.saveParticipant(existing);
        }
        for (AgendaParticipantDTO dto : participants) {
            if (isBlank(dto.getPersonName()) || isBlank(dto.getCommitteeRole())) {
                continue;
            }
            ApprovalForumAgpAgendaParticipantEntity entity = new ApprovalForumAgpAgendaParticipantEntity();
            entity.setAgiAgendaItemByAgiId(item);
            entity.setPersonId(dto.getPersonId());
            entity.setPersonName(dto.getPersonName().trim());
            entity.setEmail(dto.getEmail());
            entity.setCommitteeRole(dto.getCommitteeRole().trim());
            entity.setAttendanceRequired(dto.isAttendanceRequired());
            entity.setQuorumEligible(dto.isQuorumEligible());
            entity.setVotingEligible(dto.isVotingEligible());
            entity.setIndependentFlag(dto.isIndependentFlag());
            entity.setPlannedAttendance(dto.getPlannedAttendance());
            entity.setInvitationStatus(dto.getInvitationStatus());
            entity.setActualAttendance(dto.getActualAttendance());
            entity.setOverrideReason(dto.getOverrideReason());
            entity.setCreatedBy(user);
            entity.setCreatedOn(now());
            entity.setActive(true);
            agendaRepository.saveParticipant(entity);
        }
    }

    /**
     * AGP rows when the item has any; otherwise a read-only view synthesised from the legacy
     * roster columns so items captured before the participant table still evaluate.
     */
    private List<ApprovalForumAgpAgendaParticipantEntity> effectiveParticipants(ApprovalForumAgiAgendaItemEntity item) {
        List<ApprovalForumAgpAgendaParticipantEntity> rows = agendaRepository.getParticipantsForItem(item.getAgiId());
        if (!rows.isEmpty()) {
            return rows;
        }
        List<ApprovalForumAgpAgendaParticipantEntity> synthesized = new ArrayList<>();
        for (String[] role : LEGACY_ROSTER_ROLES) {
            String names = legacyRosterValue(item, role[1]);
            if (isBlank(names)) {
                continue;
            }
            for (String name : names.split(";")) {
                if (isBlank(name)) {
                    continue;
                }
                ApprovalForumAgpAgendaParticipantEntity p = new ApprovalForumAgpAgendaParticipantEntity();
                p.setAgiAgendaItemByAgiId(item);
                p.setPersonName(name.trim());
                p.setCommitteeRole(role[0]);
                p.setAttendanceRequired(true);
                p.setQuorumEligible(!"Scribe".equals(role[0]));
                p.setVotingEligible(!"Scribe".equals(role[0]));
                p.setIndependentFlag("IndependentMember".equals(role[0]));
                p.setActive(true);
                synthesized.add(p);
            }
        }
        return synthesized;
    }

    private String legacyRosterValue(ApprovalForumAgiAgendaItemEntity item, String field) {
        switch (field) {
            case "chair": return item.getChair();
            case "independentMember": return item.getIndependentMember();
            case "portfolioCce1": return item.getPortfolioCce1();
            case "portfolioCce2": return item.getPortfolioCce2();
            case "businessRep1": return item.getBusinessRep1();
            case "seniorBusinessRep": return item.getSeniorBusinessRep();
            case "lecRep": return item.getLecRep();
            case "eqMemberFrmExec": return item.getEqMemberFrmExec();
            case "scribe": return item.getScribe();
            default: return null;
        }
    }

    @Override
    public AgendaQuorumResultDTO evaluateQuorum(int agiId) {
        return evaluateQuorum(requireItem(agiId));
    }

    /**
     * RULE-005: quorum = distinct quorum-eligible participants holding one of the committee's
     * configured quorum roles, compared with the committee's quorum size; roles with nobody
     * assigned are reported as missing.
     */
    private AgendaQuorumResultDTO evaluateQuorum(ApprovalForumAgiAgendaItemEntity item) {
        AgendaQuorumResultDTO result = new AgendaQuorumResultDTO();
        result.setAgiId(item.getAgiId());
        ApprovalForumAgcAgendaCommitteeEntity committee = committeeOf(item);
        int required = committee == null ? 0 : committee.getQuorumSize();
        result.setRequired(required);
        result.setOverrideReason(item.getQuorumOverrideReason());
        List<String> quorumRoles = new ArrayList<>();
        if (committee != null && !isBlank(committee.getQuorumRoles())) {
            for (String role : committee.getQuorumRoles().split(";")) {
                if (!isBlank(role)) {
                    quorumRoles.add(role.trim());
                }
            }
        }
        Set<String> counted = new LinkedHashSet<>();
        Set<String> rolesSeen = new LinkedHashSet<>();
        for (ApprovalForumAgpAgendaParticipantEntity p : effectiveParticipants(item)) {
            boolean roleCounts = quorumRoles.isEmpty()
                    || quorumRoles.stream().anyMatch(r -> r.equalsIgnoreCase(p.getCommitteeRole()));
            if (!p.isQuorumEligible() || !roleCounts) {
                continue;
            }
            String key = isBlank(p.getPersonId()) ? p.getPersonName().trim().toLowerCase(Locale.ENGLISH)
                    : p.getPersonId().trim().toLowerCase(Locale.ENGLISH);
            counted.add(key);
            rolesSeen.add(p.getCommitteeRole().trim().toLowerCase(Locale.ENGLISH));
        }
        for (String role : quorumRoles) {
            if (!rolesSeen.contains(role.toLowerCase(Locale.ENGLISH))) {
                result.getMissingRoles().add(role);
            }
        }
        result.getCountedRoles().addAll(rolesSeen);
        result.setEligibleCount(counted.size());
        result.setMet(required > 0 && counted.size() >= required);
        return result;
    }

    // ---- BRS alignment: conditions & actions (BR-018, section 6.8) ----

    @Override
    public List<AgendaConditionDTO> getConditions(int agiId) {
        return agendaRepository.getConditionsForItem(agiId).stream().map(this::toConditionDTO).collect(Collectors.toList());
    }

    @Override
    public List<AgendaConditionDTO> saveConditions(int agiId, List<AgendaConditionDTO> conditions, String user) {
        ApprovalForumAgiAgendaItemEntity item = requireItem(agiId);
        assertMeetingHeld(item.getAgmAgendaMeetingByAgmId());
        if (MEETING_CLOSED.equals(item.getAgmAgendaMeetingByAgmId().getStatus())) {
            throw new IllegalStateException("Meeting is closed - conditions can no longer be edited");
        }
        Map<Integer, ApprovalForumAgcaAgendaConditionEntity> existing = new LinkedHashMap<>();
        for (ApprovalForumAgcaAgendaConditionEntity e : agendaRepository.getConditionsForItem(agiId)) {
            existing.put(e.getAgcaId(), e);
        }
        Set<Integer> kept = new LinkedHashSet<>();
        for (AgendaConditionDTO dto : conditions) {
            if (isBlank(dto.getDescription())) {
                continue;
            }
            ApprovalForumAgcaAgendaConditionEntity entity = dto.getAgcaId() > 0 ? existing.get(dto.getAgcaId()) : null;
            if (entity == null) {
                entity = new ApprovalForumAgcaAgendaConditionEntity();
                entity.setAgiAgendaItemByAgiId(item);
                entity.setCreatedBy(user);
                entity.setCreatedOn(now());
                entity.setActive(true);
                entity.setStatus(CONDITION_OPEN);
            } else if (CONDITION_CLOSED.equals(entity.getStatus()) || CONDITION_WAIVED.equals(entity.getStatus())) {
                kept.add(entity.getAgcaId());
                continue; // closed items are only changed through closeCondition (audited)
            }
            entity.setConditionType(isBlank(dto.getConditionType()) ? "Condition" : dto.getConditionType().trim());
            entity.setDescription(dto.getDescription().trim());
            entity.setOwnerId(dto.getOwnerId());
            entity.setOwnerName(dto.getOwnerName());
            entity.setDueDate(dto.getDueDate());
            entity.setEvidenceRef(dto.getEvidenceRef());
            if (!isBlank(dto.getStatus()) && !CONDITION_CLOSED.equals(dto.getStatus()) && !CONDITION_WAIVED.equals(dto.getStatus())) {
                entity.setStatus(dto.getStatus().trim());
            }
            entity = agendaRepository.saveCondition(entity);
            kept.add(entity.getAgcaId());
        }
        for (ApprovalForumAgcaAgendaConditionEntity e : existing.values()) {
            if (!kept.contains(e.getAgcaId())) {
                e.setActive(false);
                agendaRepository.saveCondition(e);
            }
        }
        return getConditions(agiId);
    }

    @Override
    public AgendaConditionDTO closeCondition(int agcaId, String status, String closureComments, String user) {
        ApprovalForumAgcaAgendaConditionEntity entity = agendaRepository.getCondition(agcaId);
        if (entity == null) {
            throw new IllegalArgumentException("Unknown condition: " + agcaId);
        }
        if (!CONDITION_CLOSED.equals(status) && !CONDITION_WAIVED.equals(status)) {
            throw new IllegalStateException("A condition can only be closed as Closed or Waived");
        }
        requireText(closureComments, "Closure comments are required when a condition is closed or waived");
        entity.setStatus(status);
        entity.setClosureComments(closureComments.trim());
        entity.setClosedBy(user);
        entity.setClosedOn(now());
        return toConditionDTO(agendaRepository.saveCondition(entity));
    }

    // ---- BRS alignment: reporting (BR-021, section 10) ----

    @Override
    public AgendaReportSummaryDTO getReportSummary(Timestamp from, Timestamp to) {
        AgendaReportSummaryDTO report = new AgendaReportSummaryDTO();
        report.setFrom(from);
        report.setTo(to);
        List<ApprovalForumAgmAgendaMeetingEntity> meetings = agendaRepository.getMeetingsBetween(from, to);
        report.setMeetings(meetings.size());
        for (ApprovalForumAgmAgendaMeetingEntity m : meetings) {
            bump(report.getMeetingsByStatus(), m.getStatus());
        }
        Timestamp today = now();
        for (ApprovalForumAgiAgendaItemEntity item : agendaRepository.getItemsBetween(from, to)) {
            report.setItems(report.getItems() + 1);
            bump(report.getItemsByStatus(), item.getItemStatus());
            ApprovalForumAgcAgendaCommitteeEntity committee = committeeOf(item);
            bump(report.getItemsByCommittee(), committee == null ? "Unknown"
                    : committee.getCommitteeName() + " " + committee.getMandateLevel());
            bump(report.getItemsByBusinessUnit(), item.getBusinessUnit());
            bump(report.getItemsByPurpose(), item.getPurposeOfSubmission());
            if (item.isLateSubmission()) {
                report.setLateItems(report.getLateItems() + 1);
                report.getLateItemDetails().add(toItemDTO(item, false));
            }
            if (!STATUS_REMOVED.equals(item.getItemStatus()) && !STATUS_REJECTED.equals(item.getItemStatus())
                    && !mandatoryDocumentsPresent(item)) {
                report.setOutstandingMandatoryPapers(report.getOutstandingMandatoryPapers() + 1);
            }
            report.setPlannedMinutes(report.getPlannedMinutes() + (item.getEstimatedMinutes() == null ? 0 : item.getEstimatedMinutes()));
            ApprovalForumAgpmAgendaPostMeetingEntity pm = agendaRepository.getPostMeetingForItem(item.getAgiId());
            if (pm != null) {
                report.setActualMinutes(report.getActualMinutes() + (pm.getActualMinutes() == null ? 0 : pm.getActualMinutes()));
                bump(report.getDecisions(), isBlank(pm.getDecisionType()) ? pm.getDecision() : pm.getDecisionType());
            }
            for (ApprovalForumAgpAgendaParticipantEntity p : effectiveParticipants(item)) {
                report.setInvited(report.getInvited() + 1);
                if (p.getPlannedAttendance() != null && !p.getPlannedAttendance().toLowerCase(Locale.ENGLISH).startsWith("dec")
                        && !p.getPlannedAttendance().toLowerCase(Locale.ENGLISH).startsWith("apol")) {
                    report.setPlannedAttending(report.getPlannedAttending() + 1);
                }
                if (Boolean.TRUE.equals(p.getActualAttendance())) {
                    report.setActuallyAttended(report.getActuallyAttended() + 1);
                }
            }
            if (STATUS_APPROVED.equals(item.getItemStatus())) {
                if (evaluateQuorum(item).isMet()) {
                    report.setQuorumMet(report.getQuorumMet() + 1);
                } else {
                    report.setQuorumNotMet(report.getQuorumNotMet() + 1);
                }
            }
            for (ApprovalForumAgcaAgendaConditionEntity c : agendaRepository.getConditionsForItem(item.getAgiId())) {
                if (!CONDITION_CLOSED.equals(c.getStatus()) && !CONDITION_WAIVED.equals(c.getStatus())) {
                    report.setOpenConditions(report.getOpenConditions() + 1);
                    if (c.getDueDate() != null && c.getDueDate().before(today)) {
                        report.setOverdueConditions(report.getOverdueConditions() + 1);
                    }
                }
            }
        }
        return report;
    }

    private void bump(Map<String, Integer> map, String key) {
        String k = isBlank(key) ? "Unspecified" : key.trim();
        map.put(k, map.getOrDefault(k, 0) + 1);
    }

    @Override
    public AgendaPostMeetingDTO unlockPostMeeting(int agiId, String user) {
        ApprovalForumAgpmAgendaPostMeetingEntity entity = agendaRepository.getPostMeetingForItem(agiId);
        if (entity == null) {
            throw new IllegalArgumentException("No post-meeting record for item: " + agiId);
        }
        entity.setLocked(false);
        entity.setLockedBy(user);
        entity.setLockedOn(now());
        return toPostMeetingDTO(agendaRepository.savePostMeeting(entity));
    }

    /**
     * Post-meeting actuals may only be captured once the meeting has taken place and been
     * explicitly marked Held (Agenda_Builder_Spec.md section 9) - both the date must have
     * passed and the secretary must have flagged the meeting as held.
     */
    private void assertMeetingHeld(ApprovalForumAgmAgendaMeetingEntity meeting) {
        if (meeting == null) {
            throw new IllegalStateException("Agenda item is not linked to a meeting");
        }
        boolean datePassed = meeting.getMeetingDate() != null && !meeting.getMeetingDate().after(now());
        if (!MEETING_HELD.equals(meeting.getStatus()) || !datePassed) {
            throw new IllegalStateException("Post-meeting details can only be captured once meeting "
                    + meeting.getAgmId() + " has taken place and been marked Held");
        }
    }

    private void assertMeetingNotLocked(ApprovalForumAgmAgendaMeetingEntity meeting) {
        if (meeting != null && MEETING_LOCKED.equals(meeting.getStatus())) {
            throw new IllegalStateException("Meeting " + meeting.getAgmId() + " is locked for editing");
        }
    }

    private void assertMeetingNotCancelled(ApprovalForumAgmAgendaMeetingEntity meeting) {
        if (meeting != null && MEETING_CANCELLED.equals(meeting.getStatus())) {
            throw new IllegalStateException("Meeting " + meeting.getAgmId() + " has been cancelled");
        }
    }

    /**
     * A discussion slot booked with explicit times may not overlap another item's slot on the
     * same meeting - a reserved time must be refused to the next analyst, not double-booked.
     * Items without (or with unparseable) times are skipped; compile proposes times for those.
     */
    private void assertNoOverlappingSlot(int agmId, Integer excludeAgiId, String startTime, String endTime) {
        Integer start = toSlotMinutes(startTime);
        Integer end = toSlotMinutes(endTime);
        if (start == null || end == null) {
            return;
        }
        if (end <= start) {
            throw new IllegalStateException("End time " + endTime + " must be after start time " + startTime);
        }
        for (ApprovalForumAgiAgendaItemEntity other : agendaRepository.getItemsForMeeting(agmId)) {
            if (STATUS_REMOVED.equals(other.getItemStatus())) {
                continue;
            }
            if (excludeAgiId != null && other.getAgiId() == excludeAgiId.intValue()) {
                continue;
            }
            Integer otherStart = toSlotMinutes(other.getStartTime());
            Integer otherEnd = toSlotMinutes(other.getEndTime());
            if (otherStart != null && otherEnd != null && start < otherEnd && otherStart < end) {
                String label = other.getAgendaNumber() != null && !other.getAgendaNumber().isEmpty()
                        ? "item " + other.getAgendaNumber() : "'" + other.getCounterpartyName() + "'";
                throw new IllegalStateException("The " + startTime + "-" + endTime + " slot is already reserved: it overlaps "
                        + label + " (" + other.getStartTime() + "-" + other.getEndTime() + ") - please pick an open slot");
            }
        }
    }

    /** "HH:mm" to minutes since midnight; null when blank or not a valid time. */
    private Integer toSlotMinutes(String time) {
        if (time == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(time.trim());
        if (!m.matches()) {
            return null;
        }
        int hour = Integer.parseInt(m.group(1));
        int minute = Integer.parseInt(m.group(2));
        return hour > 23 || minute > 59 ? null : hour * 60 + minute;
    }

    /**
     * Held is terminal: once the meeting has taken place the agenda is a final record - only
     * the post-meeting capture (savePostMeeting) remains open.
     */
    private void assertMeetingNotHeld(ApprovalForumAgmAgendaMeetingEntity meeting) {
        if (meeting != null && (MEETING_HELD.equals(meeting.getStatus()) || MEETING_CLOSED.equals(meeting.getStatus()))) {
            throw new IllegalStateException("Meeting " + meeting.getAgmId()
                    + " has been held - the agenda is final and can no longer be changed");
        }
    }

    private void applyItemFields(ApprovalForumAgiAgendaItemEntity item, AgendaItemDTO dto) {
        item.setInkombaId(dto.getInkombaId());
        item.setCounterpartyName(dto.getCounterpartyName());
        item.setBusinessUnit(dto.getBusinessUnit());
        item.setPurposeOfSubmission(dto.getPurposeOfSubmission());
        item.setPurposeDetails(dto.getPurposeDetails());
        item.setDcpZar(dto.getDcpZar());
        item.setRatingBand(dto.getRatingBand());
        item.setNumberOfPages(dto.getNumberOfPages());
        item.setWithinPageGuidance(dto.isWithinPageGuidance());
        item.setEstimatedMinutes(dto.getEstimatedMinutes());
        item.setPrimaryAnalyst(dto.getPrimaryAnalyst());
        item.setPrimaryAnalystId(dto.getPrimaryAnalystId());
        item.setReviewAnalyst(dto.getReviewAnalyst());
        item.setReviewAnalystId(dto.getReviewAnalystId());
        item.setCreditTeam(dto.getCreditTeam());
        item.setBusinessTeam(dto.getBusinessTeam());
        item.setCreditSponsor(dto.getCreditSponsor());
        item.setCreditSponsorId(dto.getCreditSponsorId());
        item.setBuHead(dto.getBuHead());
        item.setBuHeadId(dto.getBuHeadId());
        item.setLateSubmission(dto.isLateSubmission());
        item.setStartTime(dto.getStartTime());
        item.setEndTime(dto.getEndTime());
        item.setRoundRobin(dto.isRoundRobin());
        item.setChair(dto.getChair());
        item.setAttendeeEmails(dto.getAttendeeEmails());
        item.setLecRep(dto.getLecRep());
        item.setEqMemberFrmExec(dto.getEqMemberFrmExec());
        item.setBusinessRep1(dto.getBusinessRep1());
        item.setSeniorBusinessRep(dto.getSeniorBusinessRep());
        item.setPortfolioCce1(dto.getPortfolioCce1());
        item.setPortfolioCce2(dto.getPortfolioCce2());
        item.setIndependentMember(dto.getIndependentMember());
        item.setScribe(dto.getScribe());
        item.setMezzOverlayAttendees(dto.getMezzOverlayAttendees());
        item.setEsgOverlayAttendees(dto.getEsgOverlayAttendees());
        item.setAdditionalTeamAttendees(dto.getAdditionalTeamAttendees());
        item.setCountryOfRisk(dto.getCountryOfRisk());
        item.setIndustryClassification(dto.getIndustryClassification());
        item.setLecCounterparty(dto.isLecCounterparty());
        item.setDistributionSensitivities(dto.getDistributionSensitivities());
        item.setApplicantRelationship(dto.getApplicantRelationship());
        item.setApplicationType(dto.getApplicationType());
        item.setProposalReceived(dto.isProposalReceived());
        item.setProposalDistributedEmail(dto.isProposalDistributedEmail());
        item.setProposalOnDiligent(dto.isProposalOnDiligent());
        item.setComments(dto.getComments());
        item.setLateSubmissionReason(dto.getLateSubmissionReason());
    }

    /**
     * Paper submission deadline is the last Thursday 17:00 strictly before the
     * meeting date (spec section 6). Assumption to be confirmed with business.
     */
    Timestamp cutOffFor(Timestamp meetingDate) {
        LocalDate meeting = meetingDate.toLocalDateTime().toLocalDate();
        LocalDate cutOffDay = meeting.with(TemporalAdjusters.previous(CUT_OFF_DAY));
        return Timestamp.valueOf(LocalDateTime.of(cutOffDay, CUT_OFF_TIME));
    }

    /**
     * Committee-configured cut-off (day of week + time strictly before the meeting date), falling
     * back to the Thursday 17:00 default when the committee has no configuration (BRS 6.2).
     */
    public Timestamp cutOffFor(ApprovalForumAgcAgendaCommitteeEntity committee, Timestamp meetingDate) {
        if (committee == null || isBlank(committee.getCutOffDayOfWeek())) {
            return cutOffFor(meetingDate);
        }
        DayOfWeek day;
        try {
            day = parseDay(committee.getCutOffDayOfWeek());
        } catch (IllegalArgumentException ex) {
            return cutOffFor(meetingDate);
        }
        Integer minutes = toSlotMinutes(committee.getCutOffTime());
        LocalTime time = minutes == null ? CUT_OFF_TIME : LocalTime.of(minutes / 60, minutes % 60);
        LocalDate meeting = meetingDate.toLocalDateTime().toLocalDate();
        LocalDate cutOffDay = meeting.with(TemporalAdjusters.previous(day));
        return Timestamp.valueOf(LocalDateTime.of(cutOffDay, time));
    }

    private DayOfWeek parseDay(String day) {
        return DayOfWeek.valueOf(day.trim().toUpperCase(Locale.ENGLISH));
    }

    private Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    private AgendaCommitteeDTO toCommitteeDTO(ApprovalForumAgcAgendaCommitteeEntity entity) {
        AgendaCommitteeDTO dto = new AgendaCommitteeDTO();
        dto.setAgcId(entity.getAgcId());
        dto.setCommitteeName(entity.getCommitteeName());
        dto.setMandateLevel(entity.getMandateLevel());
        dto.setLevelDescription(entity.getLevelDescription());
        dto.setQuorumSize(entity.getQuorumSize());
        dto.setMeetingDay(entity.getMeetingDay());
        dto.setChair(entity.getChair());
        dto.setChairId(entity.getChairId());
        dto.setAltChair(entity.getAltChair());
        dto.setAltChairId(entity.getAltChairId());
        dto.setMeetingSecretary(entity.getMeetingSecretary());
        dto.setMeetingSecretaryId(entity.getMeetingSecretaryId());
        dto.setMinuteTaker(entity.getMinuteTaker());
        dto.setMinuteTakerId(entity.getMinuteTakerId());
        dto.setAdministrator(entity.getAdministrator());
        dto.setAdministratorId(entity.getAdministratorId());
        dto.setApproverComposition(entity.getApproverComposition());
        dto.setPageGuidance(entity.getPageGuidance());
        dto.setListName(entity.getListName());
        dto.setActive(entity.isActive());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        dto.setEffectiveTo(entity.getEffectiveTo());
        dto.setDefaultHeldVia(entity.getDefaultHeldVia());
        dto.setDefaultDurationMinutes(entity.getDefaultDurationMinutes());
        dto.setCutOffDayOfWeek(entity.getCutOffDayOfWeek());
        dto.setCutOffTime(entity.getCutOffTime());
        dto.setRequiredDocTypes(entity.getRequiredDocTypes());
        dto.setQuorumRoles(entity.getQuorumRoles());
        return dto;
    }

    private AgendaWorkflowActionDTO toActionDTO(ApprovalForumAgwAgendaWorkflowActionEntity entity) {
        AgendaWorkflowActionDTO dto = new AgendaWorkflowActionDTO();
        dto.setAgwId(entity.getAgwId());
        dto.setAgiId(entity.getAgiId());
        dto.setAgmId(entity.getAgmId());
        dto.setAction(entity.getAction());
        dto.setFromStatus(entity.getFromStatus());
        dto.setToStatus(entity.getToStatus());
        dto.setComments(entity.getComments());
        dto.setOverrideFlag(entity.isOverrideFlag());
        dto.setOverrideReason(entity.getOverrideReason());
        dto.setActionBy(entity.getCreatedBy());
        dto.setActionOn(entity.getCreatedOn());
        return dto;
    }

    private AgendaParticipantDTO toParticipantDTO(ApprovalForumAgpAgendaParticipantEntity entity) {
        AgendaParticipantDTO dto = new AgendaParticipantDTO();
        dto.setAgpId(entity.getAgpId());
        dto.setAgiId(entity.getAgiAgendaItemByAgiId() == null ? 0 : entity.getAgiAgendaItemByAgiId().getAgiId());
        dto.setPersonId(entity.getPersonId());
        dto.setPersonName(entity.getPersonName());
        dto.setEmail(entity.getEmail());
        dto.setCommitteeRole(entity.getCommitteeRole());
        dto.setAttendanceRequired(entity.isAttendanceRequired());
        dto.setQuorumEligible(entity.isQuorumEligible());
        dto.setVotingEligible(entity.isVotingEligible());
        dto.setIndependentFlag(entity.isIndependentFlag());
        dto.setPlannedAttendance(entity.getPlannedAttendance());
        dto.setInvitationStatus(entity.getInvitationStatus());
        dto.setActualAttendance(entity.getActualAttendance());
        dto.setOverrideReason(entity.getOverrideReason());
        return dto;
    }

    private AgendaConditionDTO toConditionDTO(ApprovalForumAgcaAgendaConditionEntity entity) {
        AgendaConditionDTO dto = new AgendaConditionDTO();
        dto.setAgcaId(entity.getAgcaId());
        dto.setAgiId(entity.getAgiAgendaItemByAgiId().getAgiId());
        dto.setConditionType(entity.getConditionType());
        dto.setDescription(entity.getDescription());
        dto.setOwnerId(entity.getOwnerId());
        dto.setOwnerName(entity.getOwnerName());
        dto.setDueDate(entity.getDueDate());
        dto.setStatus(entity.getStatus());
        dto.setEvidenceRef(entity.getEvidenceRef());
        dto.setClosureComments(entity.getClosureComments());
        dto.setClosedBy(entity.getClosedBy());
        dto.setClosedOn(entity.getClosedOn());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedOn(entity.getCreatedOn());
        return dto;
    }

    private AgendaMeetingRecordDTO toRecordDTO(ApprovalForumAgmrAgendaMeetingRecordEntity entity) {
        AgendaMeetingRecordDTO dto = new AgendaMeetingRecordDTO();
        dto.setAgmrId(entity.getAgmrId());
        dto.setAgmId(entity.getAgmAgendaMeetingByAgmId().getAgmId());
        dto.setRecordType(entity.getRecordType());
        dto.setName(entity.getName());
        dto.setLocation(entity.getLocation());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedOn(entity.getCreatedOn());
        return dto;
    }

    private AgendaMeetingDTO toMeetingDTO(ApprovalForumAgmAgendaMeetingEntity entity, boolean withItems) {
        AgendaMeetingDTO dto = new AgendaMeetingDTO();
        dto.setAgmId(entity.getAgmId());
        ApprovalForumAgcAgendaCommitteeEntity committee = entity.getAgcAgendaCommitteeByAgcId();
        if (committee != null) {
            dto.setAgcId(committee.getAgcId());
            dto.setCommitteeName(committee.getCommitteeName());
            dto.setMandateLevel(committee.getMandateLevel());
            dto.setQuorumSize(committee.getQuorumSize());
            dto.setChair(committee.getChair());
        }
        dto.setMeetingDate(entity.getMeetingDate());
        dto.setHeldVia(entity.getHeldVia());
        dto.setStatus(entity.getStatus());
        dto.setCutOffOn(entity.getCutOffOn());
        dto.setLockedBy(entity.getLockedBy());
        dto.setLockedOn(entity.getLockedOn());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setVenue(entity.getVenue());
        dto.setTeamsJoinUrl(entity.getTeamsJoinUrl());
        dto.setGraphEventId(entity.getGraphEventId());
        dto.setActualStartTime(entity.getActualStartTime());
        dto.setActualEndTime(entity.getActualEndTime());
        dto.setQuorumAchieved(entity.getQuorumAchieved());
        dto.setCancelledBy(entity.getCancelledBy());
        dto.setCancelledOn(entity.getCancelledOn());
        dto.setCancelReason(entity.getCancelReason());
        dto.setClosedBy(entity.getClosedBy());
        dto.setClosedOn(entity.getClosedOn());
        if (withItems) {
            dto.setRecords(agendaRepository.getRecordsForMeeting(entity.getAgmId()).stream()
                    .map(this::toRecordDTO).collect(Collectors.toList()));
            dto.setActions(agendaRepository.getWorkflowActionsForMeeting(entity.getAgmId()).stream()
                    .map(this::toActionDTO).collect(Collectors.toList()));
            dto.setItems(agendaRepository.getItemsForMeeting(entity.getAgmId()).stream()
                    .map(i -> {
                        AgendaItemDTO itemDto = toItemDTO(i, false);
                        ApprovalForumAgpmAgendaPostMeetingEntity pm =
                                agendaRepository.getPostMeetingForItem(i.getAgiId());
                        if (pm != null) {
                            itemDto.setPostMeeting(toPostMeetingDTO(pm));
                        }
                        return itemDto;
                    }).collect(Collectors.toList()));
        }
        return dto;
    }

    private AgendaItemDTO toItemDTO(ApprovalForumAgiAgendaItemEntity entity, boolean withDocuments) {
        AgendaItemDTO dto = new AgendaItemDTO();
        dto.setAgiId(entity.getAgiId());
        ApprovalForumAgmAgendaMeetingEntity meeting = entity.getAgmAgendaMeetingByAgmId();
        if (meeting != null) {
            dto.setAgmId(meeting.getAgmId());
            dto.setMeetingDate(meeting.getMeetingDate());
            dto.setMeetingStatus(meeting.getStatus());
            ApprovalForumAgcAgendaCommitteeEntity committee = meeting.getAgcAgendaCommitteeByAgcId();
            if (committee != null) {
                dto.setCommitteeName(committee.getCommitteeName());
                dto.setMandateLevel(committee.getMandateLevel());
            }
        }
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedOn(entity.getCreatedOn());
        dto.setInkombaId(entity.getInkombaId());
        dto.setCounterpartyName(entity.getCounterpartyName());
        dto.setBusinessUnit(entity.getBusinessUnit());
        dto.setPurposeOfSubmission(entity.getPurposeOfSubmission());
        dto.setPurposeDetails(entity.getPurposeDetails());
        dto.setDcpZar(entity.getDcpZar());
        dto.setRatingBand(entity.getRatingBand());
        dto.setNumberOfPages(entity.getNumberOfPages());
        dto.setWithinPageGuidance(entity.isWithinPageGuidance());
        dto.setEstimatedMinutes(entity.getEstimatedMinutes());
        dto.setPrimaryAnalyst(entity.getPrimaryAnalyst());
        dto.setPrimaryAnalystId(entity.getPrimaryAnalystId());
        dto.setReviewAnalyst(entity.getReviewAnalyst());
        dto.setReviewAnalystId(entity.getReviewAnalystId());
        dto.setCreditTeam(entity.getCreditTeam());
        dto.setBusinessTeam(entity.getBusinessTeam());
        dto.setCreditSponsor(entity.getCreditSponsor());
        dto.setCreditSponsorId(entity.getCreditSponsorId());
        dto.setBuHead(entity.getBuHead());
        dto.setBuHeadId(entity.getBuHeadId());
        dto.setLateSubmission(entity.isLateSubmission());
        dto.setLateApprovedBy(entity.getLateApprovedBy());
        dto.setLateApprovedOn(entity.getLateApprovedOn());
        dto.setOutOfGuidanceApprovedBy(entity.getOutOfGuidanceApprovedBy());
        dto.setOutOfGuidanceApprovedOn(entity.getOutOfGuidanceApprovedOn());
        dto.setItemStatus(entity.getItemStatus());
        dto.setAgendaNumber(entity.getAgendaNumber());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setRoundRobin(entity.isRoundRobin());
        dto.setChair(entity.getChair());
        dto.setAttendeeEmails(entity.getAttendeeEmails());
        dto.setLecRep(entity.getLecRep());
        dto.setEqMemberFrmExec(entity.getEqMemberFrmExec());
        dto.setBusinessRep1(entity.getBusinessRep1());
        dto.setSeniorBusinessRep(entity.getSeniorBusinessRep());
        dto.setPortfolioCce1(entity.getPortfolioCce1());
        dto.setPortfolioCce2(entity.getPortfolioCce2());
        dto.setIndependentMember(entity.getIndependentMember());
        dto.setScribe(entity.getScribe());
        dto.setMezzOverlayAttendees(entity.getMezzOverlayAttendees());
        dto.setEsgOverlayAttendees(entity.getEsgOverlayAttendees());
        dto.setAdditionalTeamAttendees(entity.getAdditionalTeamAttendees());
        dto.setCountryOfRisk(entity.getCountryOfRisk());
        dto.setIndustryClassification(entity.getIndustryClassification());
        dto.setLecCounterparty(entity.isLecCounterparty());
        dto.setDistributionSensitivities(entity.getDistributionSensitivities());
        dto.setApplicantRelationship(entity.getApplicantRelationship());
        dto.setApplicationType(entity.getApplicationType());
        dto.setProposalReceived(entity.isProposalReceived());
        dto.setProposalDistributedEmail(entity.isProposalDistributedEmail());
        dto.setProposalOnDiligent(entity.isProposalOnDiligent());
        dto.setComments(entity.getComments());
        dto.setLateSubmissionReason(entity.getLateSubmissionReason());
        dto.setSubmittedBy(entity.getSubmittedBy());
        dto.setSubmittedOn(entity.getSubmittedOn());
        dto.setQuorumOverrideReason(entity.getQuorumOverrideReason());
        if (meeting != null) {
            dto.setCutOffOn(meeting.getCutOffOn());
        }
        if (withDocuments) {
            dto.setDocuments(agendaRepository.getDocumentsForItem(entity.getAgiId()).stream()
                    .map(this::toDocumentDTO).collect(Collectors.toList()));
            dto.setParticipants(effectiveParticipants(entity).stream().map(this::toParticipantDTO).collect(Collectors.toList()));
            dto.setActions(agendaRepository.getWorkflowActionsForItem(entity.getAgiId()).stream()
                    .map(this::toActionDTO).collect(Collectors.toList()));
            dto.setConditions(agendaRepository.getConditionsForItem(entity.getAgiId()).stream()
                    .map(this::toConditionDTO).collect(Collectors.toList()));
            dto.setQuorum(evaluateQuorum(entity));
        }
        return dto;
    }

    private AgendaItemDocumentDTO toDocumentDTO(ApprovalForumAgidAgendaItemDocumentEntity entity) {
        AgendaItemDocumentDTO dto = new AgendaItemDocumentDTO();
        dto.setAgidId(entity.getAgidId());
        dto.setAgiId(entity.getAgiAgendaItemByAgiId().getAgiId());
        dto.setDocStashId(entity.getDocStashId());
        dto.setDocType(entity.getDocType());
        dto.setFileName(entity.getFileName());
        dto.setUploadedBy(entity.getUploadedBy());
        dto.setUploadedOn(entity.getUploadedOn());
        dto.setFileSize(entity.getFileSize());
        dto.setFileExtension(entity.getFileExtension());
        dto.setMandatory(entity.isMandatory());
        dto.setValidationStatus(entity.getValidationStatus());
        dto.setValidatedBy(entity.getValidatedBy());
        dto.setValidatedOn(entity.getValidatedOn());
        dto.setValidationComment(entity.getValidationComment());
        dto.setVersion(entity.getVersion());
        return dto;
    }

    private AgendaPostMeetingDTO toPostMeetingDTO(ApprovalForumAgpmAgendaPostMeetingEntity entity) {
        AgendaPostMeetingDTO dto = new AgendaPostMeetingDTO();
        dto.setAgpmId(entity.getAgpmId());
        dto.setAgiId(entity.getAgiAgendaItemByAgiId().getAgiId());
        dto.setActualPages(entity.getActualPages());
        dto.setActualMinutes(entity.getActualMinutes());
        dto.setDecision(entity.getDecision());
        dto.setCreditConditions(entity.getCreditConditions());
        dto.setAttendeesPresent(entity.getAttendeesPresent());
        dto.setMinuteTaker(entity.getMinuteTaker());
        dto.setScribe(entity.getScribe());
        dto.setAdministrator(entity.getAdministrator());
        dto.setAnalystSignOffBy(entity.getAnalystSignOffBy());
        dto.setAnalystSignOffOn(entity.getAnalystSignOffOn());
        dto.setChairSignOffBy(entity.getChairSignOffBy());
        dto.setChairSignOffOn(entity.getChairSignOffOn());
        dto.setDocStashOutputId(entity.getDocStashOutputId());
        dto.setActualRating(entity.getActualRating());
        dto.setActualPurpose(entity.getActualPurpose());
        dto.setActualDcp(entity.getActualDcp());
        dto.setDateOfCc(entity.getDateOfCc());
        dto.setLocked(entity.isLocked());
        dto.setLockedBy(entity.getLockedBy());
        dto.setLockedOn(entity.getLockedOn());
        dto.setDecisionType(entity.getDecisionType());
        dto.setDecisionDate(entity.getDecisionDate());
        return dto;
    }

}
