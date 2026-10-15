-- CASDM-1352: Credit Committee Agenda Builder tables
-- Live tables + Envers audit tables (A_ prefix), following WF_Workflow conventions.

CREATE TABLE [dbo].[AGC_AgendaCommittee]  (
    [AGC_ID]              	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]           	nvarchar(255) NOT NULL,
    [CreatedOn]           	datetime NOT NULL DEFAULT (getdate()),
    [Active]              	bit NOT NULL DEFAULT ((1)),
    [CommitteeName]       	nvarchar(255) NOT NULL,
    [MandateLevel]        	nvarchar(50) NOT NULL,
    [LevelDescription]    	nvarchar(100) NULL,
    [QuorumSize]          	int NOT NULL,
    [MeetingDay]          	nvarchar(20) NOT NULL,
    [Chair]               	nvarchar(255) NULL,
    [ChairId]             	nvarchar(255) NULL,
    [AltChair]            	nvarchar(255) NULL,
    [AltChairId]          	nvarchar(255) NULL,
    [MeetingSecretary]    	nvarchar(255) NULL,
    [MeetingSecretaryId]  	nvarchar(255) NULL,
    [MinuteTaker]         	nvarchar(255) NULL,
    [MinuteTakerId]       	nvarchar(255) NULL,
    [Administrator]       	nvarchar(255) NULL,
    [AdministratorId]     	nvarchar(255) NULL,
    [ApproverComposition] 	nvarchar(max) NULL,
    [PageGuidance]        	int NULL,
    CONSTRAINT [PK_AGC] PRIMARY KEY CLUSTERED([AGC_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[AGM_AgendaMeeting]  (
    [AGM_ID]              	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]           	nvarchar(255) NOT NULL,
    [CreatedOn]           	datetime NOT NULL DEFAULT (getdate()),
    [Active]              	bit NOT NULL DEFAULT ((1)),
    [AGC_ID]              	int NOT NULL,
    [MeetingDate]         	datetime NOT NULL,
    [HeldVia]             	nvarchar(50) NULL,
    [Status]              	nvarchar(50) NOT NULL DEFAULT ('Draft'),
    [CutOffOn]            	datetime NULL,
    [LockedBy]            	nvarchar(255) NULL,
    [LockedOn]            	datetime NULL,
    CONSTRAINT [PK_AGM] PRIMARY KEY CLUSTERED([AGM_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[AGI_AgendaItem]  (
    [AGI_ID]                  	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]               	nvarchar(255) NOT NULL,
    [CreatedOn]               	datetime NOT NULL DEFAULT (getdate()),
    [Active]                  	bit NOT NULL DEFAULT ((1)),
    [AGM_ID]                  	int NOT NULL,
    [InkombaId]               	nvarchar(100) NULL,
    [CounterpartyName]        	nvarchar(255) NOT NULL,
    [BusinessUnit]            	nvarchar(255) NULL,
    [PurposeOfSubmission]     	nvarchar(255) NOT NULL,
    [PurposeDetails]          	nvarchar(max) NULL,
    [DcpZar]                  	float NULL,
    [RatingBand]              	nvarchar(100) NULL,
    [NumberOfPages]           	int NULL,
    [WithinPageGuidance]      	bit NOT NULL DEFAULT ((1)),
    [EstimatedMinutes]        	int NULL,
    [PrimaryAnalyst]          	nvarchar(255) NULL,
    [PrimaryAnalystId]        	nvarchar(255) NULL,
    [ReviewAnalyst]           	nvarchar(255) NULL,
    [ReviewAnalystId]         	nvarchar(255) NULL,
    [CreditTeam]              	nvarchar(max) NULL,
    [BusinessTeam]            	nvarchar(max) NULL,
    [CreditSponsor]           	nvarchar(255) NULL,
    [CreditSponsorId]         	nvarchar(255) NULL,
    [BuHead]                  	nvarchar(255) NULL,
    [BuHeadId]                	nvarchar(255) NULL,
    [LateSubmission]          	bit NOT NULL DEFAULT ((0)),
    [LateApprovedBy]          	nvarchar(255) NULL,
    [LateApprovedOn]          	datetime NULL,
    [OutOfGuidanceApprovedBy] 	nvarchar(255) NULL,
    [OutOfGuidanceApprovedOn] 	datetime NULL,
    [ItemStatus]              	nvarchar(50) NOT NULL DEFAULT ('Captured'),
    [AgendaNumber]            	nvarchar(20) NULL,
    CONSTRAINT [PK_AGI] PRIMARY KEY CLUSTERED([AGI_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[AGID_AgendaItemDocument]  (
    [AGID_ID]             	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]           	nvarchar(255) NOT NULL,
    [CreatedOn]           	datetime NOT NULL DEFAULT (getdate()),
    [Active]              	bit NOT NULL DEFAULT ((1)),
    [AGI_ID]              	int NOT NULL,
    [DocStashId]          	nvarchar(255) NOT NULL,
    [FileName]            	nvarchar(255) NULL,
    [UploadedBy]          	nvarchar(255) NULL,
    [UploadedOn]          	datetime NULL,
    CONSTRAINT [PK_AGID] PRIMARY KEY CLUSTERED([AGID_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[AGPM_AgendaPostMeeting]  (
    [AGPM_ID]             	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]           	nvarchar(255) NOT NULL,
    [CreatedOn]           	datetime NOT NULL DEFAULT (getdate()),
    [Active]              	bit NOT NULL DEFAULT ((1)),
    [AGI_ID]              	int NOT NULL,
    [ActualPages]         	int NULL,
    [ActualMinutes]       	int NULL,
    [Decision]            	nvarchar(100) NULL,
    [CreditConditions]    	nvarchar(max) NULL,
    [AttendeesPresent]    	nvarchar(max) NULL,
    [MinuteTaker]         	nvarchar(255) NULL,
    [Scribe]              	nvarchar(255) NULL,
    [Administrator]       	nvarchar(255) NULL,
    [AnalystSignOffBy]    	nvarchar(255) NULL,
    [AnalystSignOffOn]    	datetime NULL,
    [ChairSignOffBy]      	nvarchar(255) NULL,
    [ChairSignOffOn]      	datetime NULL,
    [DocStashOutputId]    	nvarchar(255) NULL,
    CONSTRAINT [PK_AGPM] PRIMARY KEY CLUSTERED([AGPM_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

-- ===== Envers audit tables =====

CREATE TABLE [dbo].[A_AGC_AgendaCommittee]  (
    [AGC_ID]              	int NOT NULL,
    [REV]                 	int NOT NULL,
    [REVTYPE]             	tinyint NULL,
    [REVEND]              	int NULL,
    [CreatedBy]           	nvarchar(255) NULL,
    [CreatedOn]           	datetime NULL,
    [Active]              	bit NULL,
    [CommitteeName]       	nvarchar(255) NULL,
    [MandateLevel]        	nvarchar(50) NULL,
    [LevelDescription]    	nvarchar(100) NULL,
    [QuorumSize]          	int NULL,
    [MeetingDay]          	nvarchar(20) NULL,
    [Chair]               	nvarchar(255) NULL,
    [ChairId]             	nvarchar(255) NULL,
    [AltChair]            	nvarchar(255) NULL,
    [AltChairId]          	nvarchar(255) NULL,
    [MeetingSecretary]    	nvarchar(255) NULL,
    [MeetingSecretaryId]  	nvarchar(255) NULL,
    [MinuteTaker]         	nvarchar(255) NULL,
    [MinuteTakerId]       	nvarchar(255) NULL,
    [Administrator]       	nvarchar(255) NULL,
    [AdministratorId]     	nvarchar(255) NULL,
    [ApproverComposition] 	nvarchar(max) NULL,
    [PageGuidance]        	int NULL,
    CONSTRAINT [PK_A_AGC] PRIMARY KEY CLUSTERED([AGC_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[A_AGM_AgendaMeeting]  (
    [AGM_ID]              	int NOT NULL,
    [REV]                 	int NOT NULL,
    [REVTYPE]             	tinyint NULL,
    [REVEND]              	int NULL,
    [CreatedBy]           	nvarchar(255) NULL,
    [CreatedOn]           	datetime NULL,
    [Active]              	bit NULL,
    [AGC_ID]              	int NULL,
    [MeetingDate]         	datetime NULL,
    [HeldVia]             	nvarchar(50) NULL,
    [Status]              	nvarchar(50) NULL,
    [CutOffOn]            	datetime NULL,
    [LockedBy]            	nvarchar(255) NULL,
    [LockedOn]            	datetime NULL,
    CONSTRAINT [PK_A_AGM] PRIMARY KEY CLUSTERED([AGM_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[A_AGI_AgendaItem]  (
    [AGI_ID]                  	int NOT NULL,
    [REV]                     	int NOT NULL,
    [REVTYPE]                 	tinyint NULL,
    [REVEND]                  	int NULL,
    [CreatedBy]               	nvarchar(255) NULL,
    [CreatedOn]               	datetime NULL,
    [Active]                  	bit NULL,
    [AGM_ID]                  	int NULL,
    [InkombaId]               	nvarchar(100) NULL,
    [CounterpartyName]        	nvarchar(255) NULL,
    [BusinessUnit]            	nvarchar(255) NULL,
    [PurposeOfSubmission]     	nvarchar(255) NULL,
    [PurposeDetails]          	nvarchar(max) NULL,
    [DcpZar]                  	float NULL,
    [RatingBand]              	nvarchar(100) NULL,
    [NumberOfPages]           	int NULL,
    [WithinPageGuidance]      	bit NULL,
    [EstimatedMinutes]        	int NULL,
    [PrimaryAnalyst]          	nvarchar(255) NULL,
    [PrimaryAnalystId]        	nvarchar(255) NULL,
    [ReviewAnalyst]           	nvarchar(255) NULL,
    [ReviewAnalystId]         	nvarchar(255) NULL,
    [CreditTeam]              	nvarchar(max) NULL,
    [BusinessTeam]            	nvarchar(max) NULL,
    [CreditSponsor]           	nvarchar(255) NULL,
    [CreditSponsorId]         	nvarchar(255) NULL,
    [BuHead]                  	nvarchar(255) NULL,
    [BuHeadId]                	nvarchar(255) NULL,
    [LateSubmission]          	bit NULL,
    [LateApprovedBy]          	nvarchar(255) NULL,
    [LateApprovedOn]          	datetime NULL,
    [OutOfGuidanceApprovedBy] 	nvarchar(255) NULL,
    [OutOfGuidanceApprovedOn] 	datetime NULL,
    [ItemStatus]              	nvarchar(50) NULL,
    [AgendaNumber]            	nvarchar(20) NULL,
    CONSTRAINT [PK_A_AGI] PRIMARY KEY CLUSTERED([AGI_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[A_AGID_AgendaItemDocument]  (
    [AGID_ID]             	int NOT NULL,
    [REV]                 	int NOT NULL,
    [REVTYPE]             	tinyint NULL,
    [REVEND]              	int NULL,
    [CreatedBy]           	nvarchar(255) NULL,
    [CreatedOn]           	datetime NULL,
    [Active]              	bit NULL,
    [AGI_ID]              	int NULL,
    [DocStashId]          	nvarchar(255) NULL,
    [FileName]            	nvarchar(255) NULL,
    [UploadedBy]          	nvarchar(255) NULL,
    [UploadedOn]          	datetime NULL,
    CONSTRAINT [PK_A_AGID] PRIMARY KEY CLUSTERED([AGID_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[A_AGPM_AgendaPostMeeting]  (
    [AGPM_ID]             	int NOT NULL,
    [REV]                 	int NOT NULL,
    [REVTYPE]             	tinyint NULL,
    [REVEND]              	int NULL,
    [CreatedBy]           	nvarchar(255) NULL,
    [CreatedOn]           	datetime NULL,
    [Active]              	bit NULL,
    [AGI_ID]              	int NULL,
    [ActualPages]         	int NULL,
    [ActualMinutes]       	int NULL,
    [Decision]            	nvarchar(100) NULL,
    [CreditConditions]    	nvarchar(max) NULL,
    [AttendeesPresent]    	nvarchar(max) NULL,
    [MinuteTaker]         	nvarchar(255) NULL,
    [Scribe]              	nvarchar(255) NULL,
    [Administrator]       	nvarchar(255) NULL,
    [AnalystSignOffBy]    	nvarchar(255) NULL,
    [AnalystSignOffOn]    	datetime NULL,
    [ChairSignOffBy]      	nvarchar(255) NULL,
    [ChairSignOffOn]      	datetime NULL,
    [DocStashOutputId]    	nvarchar(255) NULL,
    CONSTRAINT [PK_A_AGPM] PRIMARY KEY CLUSTERED([AGPM_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

-- ===== Constraints =====

ALTER TABLE [dbo].[AGM_AgendaMeeting]
    ADD CONSTRAINT [FK_AGM_AGC]
   FOREIGN KEY([AGC_ID])
   REFERENCES [dbo].[AGC_AgendaCommittee]([AGC_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[AGI_AgendaItem]
    ADD CONSTRAINT [FK_AGI_AGM]
   FOREIGN KEY([AGM_ID])
   REFERENCES [dbo].[AGM_AgendaMeeting]([AGM_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[AGID_AgendaItemDocument]
    ADD CONSTRAINT [FK_AGID_AGI]
   FOREIGN KEY([AGI_ID])
   REFERENCES [dbo].[AGI_AgendaItem]([AGI_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[AGPM_AgendaPostMeeting]
    ADD CONSTRAINT [FK_AGPM_AGI]
   FOREIGN KEY([AGI_ID])
   REFERENCES [dbo].[AGI_AgendaItem]([AGI_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_AGC_AgendaCommittee]
    ADD CONSTRAINT [FK_A_AGC_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_AGC_AgendaCommittee]
    ADD CONSTRAINT [FK_A_AGC_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_AGM_AgendaMeeting]
    ADD CONSTRAINT [FK_A_AGM_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_AGM_AgendaMeeting]
    ADD CONSTRAINT [FK_A_AGM_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_AGI_AgendaItem]
    ADD CONSTRAINT [FK_A_AGI_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem]
    ADD CONSTRAINT [FK_A_AGI_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_AGID_AgendaItemDocument]
    ADD CONSTRAINT [FK_A_AGID_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_AGID_AgendaItemDocument]
    ADD CONSTRAINT [FK_A_AGID_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_AGPM_AgendaPostMeeting]
    ADD CONSTRAINT [FK_A_AGPM_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_AGPM_AgendaPostMeeting]
    ADD CONSTRAINT [FK_A_AGPM_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
