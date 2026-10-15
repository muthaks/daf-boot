package za.co.rmb.approvalforum.service.impl;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.data.ApprovalForumDataRepository;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAflmmApprovalForumListManagementMappingEntity;
import za.co.rmb.approvalforum.model.ListManagementList;
import za.co.rmb.approvalforum.model.ListManagementListUser;
import za.co.rmb.approvalforum.model.UsersResponse;
import za.co.rmb.approvalforum.service.ApprovalForumListManagementService;
import za.co.rmb.listmanagement.client.ListManagementService;
import za.co.rmb.listmanagement.model.ListResponseDTO;
import za.co.rmb.listmanagement.model.ListUser;
import za.co.rmb.listmanagement.model.ListUsersResponseDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ApprovalForumListManagementServiceImpl implements ApprovalForumListManagementService {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumListManagementServiceImpl.class);

    @Inject
    private ListManagementService listManagementService;

    private final ApprovalForumDataRepository approvalForumDataRepository;

    private Map<String, String> afSettings;

    @Inject
    public ApprovalForumListManagementServiceImpl(@Named("af_settings") Map<String, String> afSettings,
                                                  ApprovalForumDataRepository approvalForumDataRepository){
        this.afSettings = afSettings;
        this.approvalForumDataRepository = approvalForumDataRepository;
    }

    @Override
    public ListManagementList getListManagementListByName(String name) {
        ApprovalForumAflmmApprovalForumListManagementMappingEntity entity =
                approvalForumDataRepository.getListManagementMappingByName(name);
        if(entity == null){
            logger.error("List Name not Found - " + name + " - ApprovalForumListManagementServiceImpl:getListManagementListByName");
            return null;
        }
        return mapListFromEntityAndLmsResponse(entity, getListUsersFromListManagementList(entity.getListManagementId()));
    }

    @Override
    public UsersResponse getAllUsers() {
        return new UsersResponse().withUsers(getAllMappedListUsers());
    }

    /**
     * Names of every List Management list mapped into DAF (AFLMM). Committees link to one of
     * these by name - resolution happens against the mapping, so only mapped lists are offered.
     */
    @Override
    public List<String> getAllListNames() {
        List<String> names = new ArrayList<>();
        approvalForumDataRepository.getAllListManagementMappings()
                .forEach(entity -> names.add(entity.getListName()));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private List<ListManagementListUser> getAllMappedListUsers() {
        logger.info("getAllMappedListUsers(START)");
        List<String> uniqueUsers = new ArrayList<>();
        List<ListManagementListUser> listManagementListUsers = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        approvalForumDataRepository.getAllListManagementMappings().forEach(entity -> ids.add(entity.getListManagementId()));
        for(ListUser listUser: getListUsersFromListManagementLists(ids)){
            if(uniqueUsers.indexOf(listUser.getUserData().getUserId()) < 0){
                uniqueUsers.add(listUser.getUserData().getUserId());
                listManagementListUsers.add(mapListUserFromLmsUser(listUser));
            }
        }
        logger.info("getAllMappedListUsers(END)");
        return listManagementListUsers;
    }

    private List<ListUser> getListUsersFromListManagementLists(List<Integer> listManagementListIds){
        logger.info("getListUsersFromListManagementLists(START)");
        List<ListUser> listUsers = new ArrayList<>();
        ListUsersResponseDTO listUsersResponseDTO = listManagementService.getListsUsers("AXU@rmb.co.za", true, listManagementListIds);
        if(listUsersResponseDTO == null ||
                listUsersResponseDTO.getListUsers() == null ||
                listUsersResponseDTO.getListUsers().isEmpty()){
            return listUsers;
        }
        listUsers.addAll(listUsersResponseDTO.getListUsers());
        logger.info("getListUsersFromListManagementLists(END)");
        return listUsers;
    }


    private List<ListUser> getListUsersFromListManagementList(int listManagementListId){
        List<ListUser> listUsers = new ArrayList<>();
        ListResponseDTO listResponseDTO = listManagementService.getList("AXU@rmb.co.za", true, listManagementListId);
        if(listResponseDTO == null ||
                listResponseDTO.getData() == null ||
                listResponseDTO.getData().getDetailData() == null ||
                listResponseDTO.getData().getDetailData().getListUsers() == null ||
                listResponseDTO.getData().getDetailData().getListUsers().isEmpty()) return listUsers;
        return listResponseDTO.getData().getDetailData().getListUsers();
    }

    private ListManagementList mapListFromEntityAndLmsResponse(ApprovalForumAflmmApprovalForumListManagementMappingEntity entity,
                                                               List<ListUser> listUsers){
        ListManagementList list = new ListManagementList();
        list.setId(entity.getAflmmId());
        list.setListManagementId(entity.getListManagementId());
        list.setListManagementName(entity.getListName());
        list.setChairpersonList(entity.isChairpersonList());
        List<ListManagementListUser> listManagementListUsers = new ArrayList<>();
        //Play-play list to get the unique userID
        List<String> uniqueUsers = new ArrayList<>();

        for(ListUser listUser: listUsers) {
            if (!uniqueUsers.contains(listUser.getUserData().getUserId())) {
                uniqueUsers.add(listUser.getUserData().getUserId());
                listManagementListUsers.add(mapListUserFromLmsUser(listUser));
            }
        }

        listManagementListUsers.sort(Comparator.comparing(ListManagementListUser::getName));
        list.setUsers(listManagementListUsers);
        return list;
    }

    private ListManagementListUser mapListUserFromLmsUser(ListUser listUser){
        ListManagementListUser listManagementListUser = new ListManagementListUser();
        listManagementListUser.setUserId(listUser.getUserData().getUserId());
        listManagementListUser.setName(listUser.getUserData().getName());
        listManagementListUser.setSurname(listUser.getUserData().getSurname());
        listManagementListUser.setEmail(listUser.getUserData().getEmail());
        listManagementListUser.setDomain(listUser.getUserData().getDomain());
        listManagementListUser.setTitle(listUser.getUserData().getTitle());
        return listManagementListUser;
    }

}
