-- CASDM-1352: align the Agenda Builder with the Business Requirements Specification (v0.1).
-- Sections follow the BRS end-to-end workflow. Every live table change is mirrored on the
-- A_ Envers audit table; new tables get their own A_ table (same pattern as V2).

-- ===== 1. Committee & calendar configuration (BR-001, BR-002, RULE-004, RULE-007) =====
ALTER TABLE [dbo].[AGC_AgendaCommittee] ADD
    [EffectiveFrom]           datetime      NULL,
    [EffectiveTo]             datetime      NULL,
    [DefaultHeldVia]          nvarchar(50)  NULL,
    [DefaultDurationMinutes]  int           NULL,
    [CutOffDayOfWeek]         nvarchar(20)  NULL,
    [CutOffTime]              nvarchar(5)   NULL,
    [RequiredDocTypes]        nvarchar(500) NULL,
    [QuorumRoles]             nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGC_AgendaCommittee] ADD
    [EffectiveFrom]           datetime      NULL,
    [EffectiveTo]             datetime      NULL,
    [DefaultHeldVia]          nvarchar(50)  NULL,
    [DefaultDurationMinutes]  int           NULL,
    [CutOffDayOfWeek]         nvarchar(20)  NULL,
    [CutOffTime]              nvarchar(5)   NULL,
    [RequiredDocTypes]        nvarchar(500) NULL,
    [QuorumRoles]             nvarchar(1000) NULL
GO
-- Existing committees keep today's behaviour: Thursday 17:00 cut-off, credit paper mandatory,
-- quorum counted over the standard/enhanced roster roles captured on an item.
UPDATE [dbo].[AGC_AgendaCommittee]
   SET [CutOffDayOfWeek] = 'Thursday',
       [CutOffTime] = '17:00',
       [RequiredDocTypes] = 'Credit Paper',
       [QuorumRoles] = 'Chair;IndependentMember;PortfolioCce1;PortfolioCce2;BusinessRep1;SeniorBusinessRep;LecRep;EqMemberFrmExec'
 WHERE [CutOffDayOfWeek] IS NULL
GO

ALTER TABLE [dbo].[AGM_AgendaMeeting] ADD
    [StartTime]        nvarchar(5)    NULL,
    [EndTime]          nvarchar(5)    NULL,
    [Venue]            nvarchar(255)  NULL,
    [TeamsJoinUrl]     nvarchar(1000) NULL,
    [GraphEventId]     nvarchar(255)  NULL,
    [ActualStartTime]  nvarchar(5)    NULL,
    [ActualEndTime]    nvarchar(5)    NULL,
    [QuorumAchieved]   bit            NULL,
    [CancelledBy]      nvarchar(255)  NULL,
    [CancelledOn]      datetime       NULL,
    [CancelReason]     nvarchar(1000) NULL,
    [ClosedBy]         nvarchar(255)  NULL,
    [ClosedOn]         datetime       NULL
GO
ALTER TABLE [dbo].[A_AGM_AgendaMeeting] ADD
    [StartTime]        nvarchar(5)    NULL,
    [EndTime]          nvarchar(5)    NULL,
    [Venue]            nvarchar(255)  NULL,
    [TeamsJoinUrl]     nvarchar(1000) NULL,
    [GraphEventId]     nvarchar(255)  NULL,
    [ActualStartTime]  nvarchar(5)    NULL,
    [ActualEndTime]    nvarchar(5)    NULL,
    [QuorumAchieved]   bit            NULL,
    [CancelledBy]      nvarchar(255)  NULL,
    [CancelledOn]      datetime       NULL,
    [CancelReason]     nvarchar(1000) NULL,
    [ClosedBy]         nvarchar(255)  NULL,
    [ClosedOn]         datetime       NULL
GO

-- ===== 2. Late submission & readiness (BR-006, BR-007, RULE-003) =====
ALTER TABLE [dbo].[AGI_AgendaItem] ADD
    [SubmittedBy]          nvarchar(255)  NULL,
    [SubmittedOn]          datetime       NULL,
    [QuorumOverrideReason] nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ADD
    [SubmittedBy]          nvarchar(255)  NULL,
    [SubmittedOn]          datetime       NULL,
    [QuorumOverrideReason] nvarchar(1000) NULL
GO

-- ===== 3. Documents (section 6.5) =====
ALTER TABLE [dbo].[AGID_AgendaItemDocument] ADD
    [Mandatory]         bit           NOT NULL DEFAULT ((0)),
    [ValidationStatus]  nvarchar(20)  NULL,
    [ValidatedBy]       nvarchar(255) NULL,
    [ValidatedOn]       datetime      NULL,
    [ValidationComment] nvarchar(1000) NULL,
    [Version]           int           NOT NULL DEFAULT ((1))
GO
ALTER TABLE [dbo].[A_AGID_AgendaItemDocument] ADD
    [Mandatory]         bit           NULL,
    [ValidationStatus]  nvarchar(20)  NULL,
    [ValidatedBy]       nvarchar(255) NULL,
    [ValidatedOn]       datetime      NULL,
    [ValidationComment] nvarchar(1000) NULL,
    [Version]           int           NULL
GO

-- ===== 4. Review & approval workflow (BR-010, section 6.6, RULE-009) =====
CREATE TABLE [dbo].[AGW_AgendaWorkflowAction]  (
    [AGW_ID]          	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]       	nvarchar(255) NOT NULL,
    [CreatedOn]       	datetime NOT NULL DEFAULT (getdate()),
    [Active]          	bit NOT NULL DEFAULT ((1)),
    [AGI_ID]          	int NULL,
    [AGM_ID]          	int NULL,
    [Action]          	nvarchar(50) NOT NULL,
    [FromStatus]      	nvarchar(50) NULL,
    [ToStatus]        	nvarchar(50) NULL,
    [Comments]        	nvarchar(max) NULL,
    [OverrideFlag]    	bit NOT NULL DEFAULT ((0)),
    [OverrideReason]  	nvarchar(1000) NULL,
    CONSTRAINT [PK_AGW] PRIMARY KEY CLUSTERED([AGW_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO
CREATE TABLE [dbo].[A_AGW_AgendaWorkflowAction]  (
    [AGW_ID]          	int NOT NULL,
    [REV]             	int NOT NULL,
    [REVTYPE]         	tinyint NULL,
    [REVEND]          	int NULL,
    [CreatedBy]       	nvarchar(255) NULL,
    [CreatedOn]       	datetime NULL,
    [Active]          	bit NULL,
    [AGI_ID]          	int NULL,
    [AGM_ID]          	int NULL,
    [Action]          	nvarchar(50) NULL,
    [FromStatus]      	nvarchar(50) NULL,
    [ToStatus]        	nvarchar(50) NULL,
    [Comments]        	nvarchar(max) NULL,
    [OverrideFlag]    	bit NULL,
    [OverrideReason]  	nvarchar(1000) NULL,
    CONSTRAINT [PK_A_AGW] PRIMARY KEY CLUSTERED([AGW_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

-- ===== 5. Participants, attendance & quorum (BR-008, BR-009, RULE-005, RULE-006, section 6.4) =====
CREATE TABLE [dbo].[AGP_AgendaParticipant]  (
    [AGP_ID]              	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]           	nvarchar(255) NOT NULL,
    [CreatedOn]           	datetime NOT NULL DEFAULT (getdate()),
    [Active]              	bit NOT NULL DEFAULT ((1)),
    [AGI_ID]              	int NOT NULL,
    [PersonId]            	nvarchar(255) NULL,
    [PersonName]          	nvarchar(255) NOT NULL,
    [Email]               	nvarchar(255) NULL,
    [CommitteeRole]       	nvarchar(100) NOT NULL,
    [AttendanceRequired]  	bit NOT NULL DEFAULT ((1)),
    [QuorumEligible]      	bit NOT NULL DEFAULT ((1)),
    [VotingEligible]      	bit NOT NULL DEFAULT ((1)),
    [IndependentFlag]     	bit NOT NULL DEFAULT ((0)),
    [PlannedAttendance]   	nvarchar(20) NULL,
    [InvitationStatus]    	nvarchar(20) NULL,
    [ActualAttendance]    	bit NULL,
    [OverrideReason]      	nvarchar(1000) NULL,
    CONSTRAINT [PK_AGP] PRIMARY KEY CLUSTERED([AGP_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO
CREATE TABLE [dbo].[A_AGP_AgendaParticipant]  (
    [AGP_ID]              	int NOT NULL,
    [REV]                 	int NOT NULL,
    [REVTYPE]             	tinyint NULL,
    [REVEND]              	int NULL,
    [CreatedBy]           	nvarchar(255) NULL,
    [CreatedOn]           	datetime NULL,
    [Active]              	bit NULL,
    [AGI_ID]              	int NULL,
    [PersonId]            	nvarchar(255) NULL,
    [PersonName]          	nvarchar(255) NULL,
    [Email]               	nvarchar(255) NULL,
    [CommitteeRole]       	nvarchar(100) NULL,
    [AttendanceRequired]  	bit NULL,
    [QuorumEligible]      	bit NULL,
    [VotingEligible]      	bit NULL,
    [IndependentFlag]     	bit NULL,
    [PlannedAttendance]   	nvarchar(20) NULL,
    [InvitationStatus]    	nvarchar(20) NULL,
    [ActualAttendance]    	bit NULL,
    [OverrideReason]      	nvarchar(1000) NULL,
    CONSTRAINT [PK_A_AGP] PRIMARY KEY CLUSTERED([AGP_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

-- ===== 6. Decisions & conditions (BR-017, BR-018, section 6.7 / 6.8) =====
ALTER TABLE [dbo].[AGPM_AgendaPostMeeting] ADD
    [DecisionType]  nvarchar(50) NULL,
    [DecisionDate]  datetime     NULL
GO
ALTER TABLE [dbo].[A_AGPM_AgendaPostMeeting] ADD
    [DecisionType]  nvarchar(50) NULL,
    [DecisionDate]  datetime     NULL
GO
CREATE TABLE [dbo].[AGCA_AgendaCondition]  (
    [AGCA_ID]         	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]       	nvarchar(255) NOT NULL,
    [CreatedOn]       	datetime NOT NULL DEFAULT (getdate()),
    [Active]          	bit NOT NULL DEFAULT ((1)),
    [AGI_ID]          	int NOT NULL,
    [ConditionType]   	nvarchar(50) NOT NULL,
    [Description]     	nvarchar(max) NOT NULL,
    [OwnerId]         	nvarchar(255) NULL,
    [OwnerName]       	nvarchar(255) NULL,
    [DueDate]         	datetime NULL,
    [Status]          	nvarchar(50) NOT NULL DEFAULT ('Open'),
    [EvidenceRef]     	nvarchar(1000) NULL,
    [ClosureComments] 	nvarchar(max) NULL,
    [ClosedBy]        	nvarchar(255) NULL,
    [ClosedOn]        	datetime NULL,
    CONSTRAINT [PK_AGCA] PRIMARY KEY CLUSTERED([AGCA_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO
CREATE TABLE [dbo].[A_AGCA_AgendaCondition]  (
    [AGCA_ID]         	int NOT NULL,
    [REV]             	int NOT NULL,
    [REVTYPE]         	tinyint NULL,
    [REVEND]          	int NULL,
    [CreatedBy]       	nvarchar(255) NULL,
    [CreatedOn]       	datetime NULL,
    [Active]          	bit NULL,
    [AGI_ID]          	int NULL,
    [ConditionType]   	nvarchar(50) NULL,
    [Description]     	nvarchar(max) NULL,
    [OwnerId]         	nvarchar(255) NULL,
    [OwnerName]       	nvarchar(255) NULL,
    [DueDate]         	datetime NULL,
    [Status]          	nvarchar(50) NULL,
    [EvidenceRef]     	nvarchar(1000) NULL,
    [ClosureComments] 	nvarchar(max) NULL,
    [ClosedBy]        	nvarchar(255) NULL,
    [ClosedOn]        	datetime NULL,
    CONSTRAINT [PK_A_AGCA] PRIMARY KEY CLUSTERED([AGCA_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

-- ===== 7. Records & evidence (BR-020) =====
CREATE TABLE [dbo].[AGMR_AgendaMeetingRecord]  (
    [AGMR_ID]     	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]   	nvarchar(255) NOT NULL,
    [CreatedOn]   	datetime NOT NULL DEFAULT (getdate()),
    [Active]      	bit NOT NULL DEFAULT ((1)),
    [AGM_ID]      	int NOT NULL,
    [RecordType]  	nvarchar(50) NOT NULL,
    [Name]        	nvarchar(255) NOT NULL,
    [Location]    	nvarchar(1000) NOT NULL,
    CONSTRAINT [PK_AGMR] PRIMARY KEY CLUSTERED([AGMR_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO
CREATE TABLE [dbo].[A_AGMR_AgendaMeetingRecord]  (
    [AGMR_ID]     	int NOT NULL,
    [REV]         	int NOT NULL,
    [REVTYPE]     	tinyint NULL,
    [REVEND]      	int NULL,
    [CreatedBy]   	nvarchar(255) NULL,
    [CreatedOn]   	datetime NULL,
    [Active]      	bit NULL,
    [AGM_ID]      	int NULL,
    [RecordType]  	nvarchar(50) NULL,
    [Name]        	nvarchar(255) NULL,
    [Location]    	nvarchar(1000) NULL,
    CONSTRAINT [PK_A_AGMR] PRIMARY KEY CLUSTERED([AGMR_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

ALTER TABLE [dbo].[AGW_AgendaWorkflowAction] ADD CONSTRAINT [FK_AGW_AGI]
    FOREIGN KEY ([AGI_ID]) REFERENCES [dbo].[AGI_AgendaItem]([AGI_ID])
GO
ALTER TABLE [dbo].[AGP_AgendaParticipant] ADD CONSTRAINT [FK_AGP_AGI]
    FOREIGN KEY ([AGI_ID]) REFERENCES [dbo].[AGI_AgendaItem]([AGI_ID])
GO
ALTER TABLE [dbo].[AGCA_AgendaCondition] ADD CONSTRAINT [FK_AGCA_AGI]
    FOREIGN KEY ([AGI_ID]) REFERENCES [dbo].[AGI_AgendaItem]([AGI_ID])
GO
ALTER TABLE [dbo].[AGMR_AgendaMeetingRecord] ADD CONSTRAINT [FK_AGMR_AGM]
    FOREIGN KEY ([AGM_ID]) REFERENCES [dbo].[AGM_AgendaMeeting]([AGM_ID])
GO
