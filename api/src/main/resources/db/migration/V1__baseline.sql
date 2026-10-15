-- Flyway V1 baseline: consolidated from legacy scripts in src/main/resources/sql

-- ===== tables.sql =====
CREATE TABLE [dbo].[A_WF_Workflow]  ( 
    [WF_ID]               	int NOT NULL,
    [REV]                 	int NOT NULL,
    [REVTYPE]             	tinyint NULL,
    [REVEND]              	int NULL,
    [CreatedBy]           	nvarchar(255) NOT NULL,
    [CreatedOn]           	datetime NOT NULL DEFAULT (getdate()),
    [Active]              	bit NOT NULL DEFAULT ((1)),
    [RT_ID]               	int NOT NULL,
    [Confidential]        	bit NOT NULL DEFAULT ((0)),
    [Status]              	nvarchar(255) NULL,
    [Specific]            	bit NOT NULL DEFAULT ((0)),
    [SpecificReason]      	nvarchar(max) NULL,
    [SubmittedBy]         	nvarchar(255) NULL,
    [SubmittedOn]         	datetime NULL,
    [CompletedOn]         	datetime NULL,
    [FirstEscalationSent] 	bit NOT NULL DEFAULT ((0)),
    [SecondEscalationSent]	bit NOT NULL DEFAULT ((0)),
    [AssignedTo]          	nvarchar(255) NULL,
    [AssignedOn]          	datetime NULL,
    [ReferencedWorkflowId]	int NULL,
    [CaLastSeenChatId]    	int NOT NULL,
    [RaLastSeenChatId]    	int NOT NULL,
    CONSTRAINT [PK_A_WF] PRIMARY KEY CLUSTERED([WF_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[A_WFA_WorkflowApprovals]  ( 
    [WFA_ID]                          	int NOT NULL,
    [REV]                             	int NOT NULL,
    [REVTYPE]                         	tinyint NULL,
    [REVEND]                          	int NULL,
    [WF_ID]                           	int NOT NULL,
    [UserId]                          	nvarchar(255) NOT NULL,
    [Name]                            	nvarchar(255) NULL,
    [Surname]                         	nvarchar(255) NULL,
    [Email]                           	nvarchar(255) NULL,
    [Title]                           	nvarchar(100) NULL,
    [Domain]                          	nvarchar(100) NULL,
    [DateAssigned]                    	datetime NULL,
    [DateCompleted]                   	datetime NULL,
    [Decision]                        	nvarchar(100) NULL,
    [AFLMM_ID]                        	int NULL,
    [OnlyForDistribution]             	bit NOT NULL DEFAULT ((0)),
    [SpecificApprover]                	bit NOT NULL DEFAULT ((0)),
    [AdditionalApprover]              	bit NOT NULL DEFAULT ((0)),
    [ApprovalConditions]              	nvarchar(max) NULL,
    [RecommendedPolicyApprovalMandate]	nvarchar(255) NULL,
    [LastSeenChatId]                  	int NOT NULL,
    CONSTRAINT [PK_A_WFA] PRIMARY KEY CLUSTERED([WFA_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[A_WFAD_WorkflowApplicationData]  ( 
    [WFAD_ID]                   	int NOT NULL,
    [REV]                       	int NOT NULL,
    [REVTYPE]                   	tinyint NULL,
    [REVEND]                    	int NULL,
    [WFCD_ID]                   	int NOT NULL,
    [LastApprovedFrRating]      	nvarchar(100) NULL,
    [ProposedFrRating]          	nvarchar(100) NULL,
    [LastApprovedDcp]           	float NULL,
    [LastApprovedSettlementDcp] 	float NULL,
    [ProposedDcp]               	float NULL,
    [ProposedSettlementDcp]     	float NULL,
    [DistressStatus]            	nvarchar(255) NULL,
    [WholesaleCreditPortfolio]  	nvarchar(255) NULL,
    [MeetingName]               	nvarchar(255) NULL,
    [ProposalType]              	nvarchar(255) NULL,
    [ProposalAction]            	nvarchar(255) NULL,
    [ReviewAnalyst]             	nvarchar(255) NULL,
    [ReviewAnalystId]           	nvarchar(255) NULL,
    [ReviewAnalystEmail]        	nvarchar(255) NULL,
    [ApplicationId]             	int NULL,
    [CreditAnalyst]             	nvarchar(255) NULL,
    [CreditAnalystId]           	nvarchar(255) NULL,
    [CreditAnalystEmail]        	nvarchar(255) NULL,
    [ReviewAnalystSignatureDate]	datetime NULL,
    [CreditAnalystSignatureDate]	datetime NULL,
    CONSTRAINT [PK_A_WFAD] PRIMARY KEY CLUSTERED([WFAD_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[A_WFC_WorkflowChat]  ( 
    [WFC_ID]     	int NOT NULL,
    [REV]        	int NOT NULL,
    [REVTYPE]    	tinyint NULL,
    [REVEND]     	int NULL,
    [WF_ID]      	int NOT NULL,
    [CreatedOn]  	datetime NOT NULL DEFAULT (getdate()),
    [UserId]     	nvarchar(255) NOT NULL,
    [DisplayName]	nvarchar(255) NOT NULL,
    [ChatText]   	nvarchar(max) NOT NULL,
    CONSTRAINT [PK_A_WFC] PRIMARY KEY CLUSTERED([WFC_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[A_WFCD_WorkflowCounterpartyDetail]  ( 
    [WFCD_ID]           	int NOT NULL,
    [REV]               	int NOT NULL,
    [REVTYPE]           	tinyint NULL,
    [REVEND]            	int NULL,
    [WF_ID]             	int NOT NULL,
    [CreditInkombaId]   	nvarchar(255) NOT NULL,
    [CounterpartyName]  	nvarchar(255) NOT NULL,
    [CreditAnalyst]     	nvarchar(255) NULL,
    [CreditAnalystId]   	nvarchar(255) NULL,
    [CreditAnalystEmail]	nvarchar(255) NULL,
    [Bank]              	bit NOT NULL DEFAULT ((0)),
    [India]             	bit NOT NULL DEFAULT ((0)), -- no longer used
    [FinancialInstitutionType]  nvarchar(255) NOT NULL DEFAULT (''),
    CONSTRAINT [PK_A_WFCD] PRIMARY KEY CLUSTERED([WFCD_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[A_WFDA_WorkflowData]  ( 
    [WFDA_ID]       	int NOT NULL,
    [REV]           	int NOT NULL,
    [REVTYPE]       	tinyint NULL,
    [REVEND]        	int NULL,
    [WF_ID]         	int NOT NULL,
    [Recommendation]	nvarchar(max) NOT NULL,
    CONSTRAINT [PK_A_WFDA] PRIMARY KEY CLUSTERED([WFDA_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[A_WFFI_WorkflowFacilityInformation]  ( 
    [WFFI_ID]                	int NOT NULL,
    [REV]                    	int NOT NULL,
    [REVTYPE]                	tinyint NULL,
    [REVEND]                 	int NULL,
    [WFCD_ID]                	int NOT NULL,
    [FacilityReference]      	nvarchar(1024) NULL,
    [RiskType]               	nvarchar(50) NOT NULL,
    [Tenor]                  	nvarchar(30) NOT NULL,
    [LocalInt]               	nvarchar(50) NOT NULL, -- no longer user
    [Currency]               	nvarchar(50) NOT NULL,
    [NetLimit]               	float NOT NULL,
    [FacilityDescription]    	nvarchar(1024) NULL,
    [CreditApplicationStatus]	nvarchar(50) NOT NULL,
    [RiskTaker]              	nvarchar(250) NOT NULL,
    [DealSpecific]           	bit NOT NULL DEFAULT ((1)),
    [ProposedRating]         	nvarchar(50) NULL,
    [Lgd]                    	float NOT NULL,
    [FacilityId]             	nvarchar(50) NOT NULL,
    [FacilityRating]         	nvarchar(30) NULL,
    [FacilityInvestmentType] 	nvarchar(250) NULL,
    [NetLimitZar]            	float NOT NULL,
    [LgdZar]                 	float NOT NULL,
    [RiskTakerGroupCode]        nvarchar(3) NOT NULL DEFAULT ('')
    CONSTRAINT [PK_A_WFFI] PRIMARY KEY CLUSTERED([WFFI_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[A_WFQ_WorkflowQuorum]  ( 
    [WFQ_ID]                        	int NOT NULL,
    [REV]                           	int NOT NULL,
    [REVTYPE]                       	tinyint NULL,
    [REVEND]                        	int NULL,
    [WF_ID]                         	int NOT NULL,
    [TotalNumberOfApprovalsRequired]	int NOT NULL,
    [Selected]                      	bit NOT NULL DEFAULT ((0)),
    CONSTRAINT [PK_A_WFQ] PRIMARY KEY CLUSTERED([WFQ_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[A_WFQD_WorkflowQuorumData]  ( 
    [WFQD_ID]    	int NOT NULL,
    [REV]        	int NOT NULL,
    [REVTYPE]    	tinyint NULL,
    [REVEND]     	int NULL,
    [WFQ_ID]     	int NOT NULL,
    [AFLMM_ID]   	int NOT NULL,
    [MinRequired]	int NOT NULL,
    [MaxRequired]	int NOT NULL,
    [Rank]       	int NOT NULL,
    CONSTRAINT [PK_A_WFQD] PRIMARY KEY CLUSTERED([WFQD_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[ADU_ActiveDirectoryUser]  ( 
    [ADU_ID]     	int IDENTITY(1,1) NOT NULL,
    [Alias]      	nvarchar(100) NULL,
    [DisplayName]	nvarchar(100) NULL,
    [GivenName]  	nvarchar(100) NULL,
    [Surname]    	nvarchar(100) NULL,
    [Mail]       	nvarchar(100) NULL,
    [CreatedOn]  	datetime NULL DEFAULT (getdate()),
    [UpdatedOn]  	datetime NULL,
    CONSTRAINT [PK_ADU_ID] PRIMARY KEY CLUSTERED([ADU_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[AFADG_ApprovalForumActiveDirectoryGroup]  ( 
    [AFADG_ID]	int IDENTITY(1,1) NOT NULL,
    [AFG_ID]  	int NOT NULL,
    [ADGroup] 	varchar(255) NOT NULL,
    CONSTRAINT [PK_AFADG] PRIMARY KEY CLUSTERED([AFADG_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[AFG_ApprovalForumGroup]  ( 
    [AFG_ID]   	int IDENTITY(1,1) NOT NULL,
    [GroupName]	varchar(100) NOT NULL,
    CONSTRAINT [PK_AFG] PRIMARY KEY CLUSTERED([AFG_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[AFLMM_ApprovalForumListManagementMapping]  ( 
    [AFLMM_ID]        	int IDENTITY(1,1) NOT NULL,
    [ListName]        	varchar(255) NOT NULL,
    [ListManagementId]	int NOT NULL,
    CONSTRAINT [PK_AFLMM] PRIMARY KEY CLUSTERED([AFLMM_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[DSR_DocStashRequests]  ( 
    [DSR_ID]            	int IDENTITY(1,1) NOT NULL,
    [CreatedOn]         	datetime NOT NULL DEFAULT (getdate()),
    [IsComplete]        	bit NOT NULL DEFAULT ((1)),
    [CompletedOn]       	datetime NULL,
    [LastTriedOn]       	datetime NULL,
    [LastTriedMessage]  	nvarchar(max) NULL,
    [RetryCount]        	int NULL,
    [WF_ID]             	int NULL,
    [DocStashDocumentId]	int NULL,
    CONSTRAINT [PK_DSR] PRIMARY KEY CLUSTERED([DSR_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[ER_ExternalRequests]  ( 
    [ER_ID]           	int IDENTITY(1,1) NOT NULL,
    [IsCreate]        	bit NOT NULL DEFAULT ((1)),
    [ApplicationId]   	int NULL,
    [CreatedBy]       	nvarchar(255) NOT NULL,
    [CreatedOn]       	datetime NOT NULL DEFAULT (getdate()),
    [IsComplete]      	bit NOT NULL DEFAULT ((1)),
    [CompletedOn]     	datetime NULL,
    [LastTriedOn]     	datetime NULL,
    [LastTriedMessage]	nvarchar(max) NULL,
    [RetryCount]      	int NULL,
    [WF_ID]           	int NULL,
    CONSTRAINT [PK_ER] PRIMARY KEY CLUSTERED([ER_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[MR_MailRequests]  ( 
    [MR_ID]           	int IDENTITY(1,1) NOT NULL,
    [CreatedOn]       	datetime NOT NULL DEFAULT (getdate()),
    [IsComplete]      	bit NOT NULL DEFAULT ((1)),
    [CompletedOn]     	datetime NULL,
    [LastTriedOn]     	datetime NULL,
    [LastTriedMessage]	nvarchar(max) NULL,
    [RetryCount]      	int NULL,
    [WF_ID]           	int NULL,
    [MailModel]       	varbinary(max) NULL,
    [MailProcess]     	nvarchar(255) NULL,
    CONSTRAINT [PK_MR] PRIMARY KEY CLUSTERED([MR_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[P_Properties]  ( 
    [P_ID] 	int IDENTITY(1,1) NOT NULL,
    [Name] 	varchar(255) NULL,
    [Value]	varchar(512) NULL,
    CONSTRAINT [PK_P] PRIMARY KEY CLUSTERED([P_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[RI_RevisionInfo]  ( 
    [Rev]        	int IDENTITY(1,1) NOT NULL,
    [RevTstmp]   	bigint NOT NULL,
    [UserName]   	varchar(100) NOT NULL,
    [Description]	varchar(255) NOT NULL,
    [WorkflowId] 	int NOT NULL,
    CONSTRAINT [PK_RI] PRIMARY KEY CLUSTERED([Rev])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[RT_RequestType]  ( 
    [RT_ID]	int IDENTITY(1,1) NOT NULL,
    [Type] 	varchar(255) NOT NULL,
    CONSTRAINT [PK_RT] PRIMARY KEY CLUSTERED([RT_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[WF_Workflow]  ( 
    [WF_ID]               	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]           	nvarchar(255) NOT NULL,
    [CreatedOn]           	datetime NOT NULL DEFAULT (getdate()),
    [Active]              	bit NOT NULL DEFAULT ((1)),
    [RT_ID]               	int NOT NULL,
    [Confidential]        	bit NOT NULL DEFAULT ((0)),
    [Status]              	nvarchar(255) NULL,
    [Specific]            	bit NOT NULL DEFAULT ((0)),
    [SpecificReason]      	nvarchar(max) NULL,
    [SubmittedBy]         	nvarchar(255) NULL,
    [SubmittedOn]         	datetime NULL,
    [CompletedOn]         	datetime NULL,
    [FirstEscalationSent] 	bit NOT NULL DEFAULT ((0)),
    [SecondEscalationSent]	bit NOT NULL DEFAULT ((0)),
    [AssignedTo]          	nvarchar(255) NULL,
    [AssignedOn]          	datetime NULL,
    [ReferencedWorkflowId]	int NULL,
    [CaLastSeenChatId]    	int NOT NULL,
    [RaLastSeenChatId]    	int NOT NULL,
    CONSTRAINT [PK_WF] PRIMARY KEY CLUSTERED([WF_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[WFA_WorkflowApprovals]  ( 
    [WFA_ID]                          	int IDENTITY(1,1) NOT NULL,
    [WF_ID]                           	int NOT NULL,
    [UserId]                          	nvarchar(255) NOT NULL,
    [Name]                            	nvarchar(255) NULL,
    [Surname]                         	nvarchar(255) NULL,
    [Email]                           	nvarchar(255) NULL,
    [Title]                           	nvarchar(100) NULL,
    [Domain]                          	nvarchar(100) NULL,
    [DateAssigned]                    	datetime NULL,
    [DateCompleted]                   	datetime NULL,
    [Decision]                        	nvarchar(100) NULL,
    [AFLMM_ID]                        	int NULL,
    [OnlyForDistribution]             	bit NOT NULL DEFAULT ((0)),
    [SpecificApprover]                	bit NOT NULL DEFAULT ((0)),
    [AdditionalApprover]              	bit NOT NULL DEFAULT ((0)),
    [ApprovalConditions]              	nvarchar(max) NULL,
    [RecommendedPolicyApprovalMandate]	nvarchar(255) NULL,
    [LastSeenChatId]                  	int NOT NULL,
    CONSTRAINT [PK_WFA] PRIMARY KEY CLUSTERED([WFA_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[WFAD_WorkflowApplicationData]  ( 
    [WFAD_ID]                   	int IDENTITY(1,1) NOT NULL,
    [WFCD_ID]                   	int NOT NULL,
    [LastApprovedFrRating]      	nvarchar(100) NULL,
    [ProposedFrRating]          	nvarchar(100) NULL,
    [LastApprovedDcp]           	float NULL,
    [LastApprovedSettlementDcp] 	float NULL,
    [ProposedDcp]               	float NULL,
    [ProposedSettlementDcp]     	float NULL,
    [DistressStatus]            	nvarchar(255) NULL,
    [WholesaleCreditPortfolio]  	nvarchar(255) NULL,
    [MeetingName]               	nvarchar(255) NULL,
    [ProposalType]              	nvarchar(255) NULL,
    [ProposalAction]            	nvarchar(255) NULL,
    [ReviewAnalyst]             	nvarchar(255) NULL,
    [ReviewAnalystId]           	nvarchar(255) NULL,
    [ReviewAnalystEmail]        	nvarchar(255) NULL,
    [ApplicationId]             	int NULL,
    [CreditAnalyst]             	nvarchar(255) NULL,
    [CreditAnalystId]           	nvarchar(255) NULL,
    [CreditAnalystEmail]        	nvarchar(255) NULL,
    [ReviewAnalystSignatureDate]	datetime NULL,
    [CreditAnalystSignatureDate]	datetime NULL,
    CONSTRAINT [PK_WFAD] PRIMARY KEY CLUSTERED([WFAD_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[WFC_WorkflowChat]  ( 
    [WFC_ID]     	int IDENTITY(1,1) NOT NULL,
    [WF_ID]      	int NOT NULL,
    [CreatedOn]  	datetime NOT NULL DEFAULT (getdate()),
    [UserId]     	nvarchar(255) NOT NULL,
    [DisplayName]	nvarchar(255) NOT NULL,
    [ChatText]   	nvarchar(max) NOT NULL,
    CONSTRAINT [PK_WFC] PRIMARY KEY CLUSTERED([WFC_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[WFCD_WorkflowCounterpartyDetail]  ( 
    [WFCD_ID]           	int IDENTITY(1,1) NOT NULL,
    [WF_ID]             	int NOT NULL,
    [CreditInkombaId]   	nvarchar(255) NOT NULL,
    [CounterpartyName]  	nvarchar(255) NOT NULL,
    [CreditAnalyst]     	nvarchar(255) NULL,
    [CreditAnalystId]   	nvarchar(255) NULL,
    [CreditAnalystEmail]	nvarchar(255) NULL,
    [Bank]              	bit NOT NULL DEFAULT ((0)),
    [India]             	bit NOT NULL DEFAULT ((0)), -- no longer used
    [FinancialInstitutionType]  nvarchar(255) NULL,
    CONSTRAINT [PK_WFCD] PRIMARY KEY CLUSTERED([WFCD_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[WFD_WorkflowDocument]  ( 
    [WFD_ID]        	int IDENTITY(1,1) NOT NULL,
    [WorkflowID]    	int NULL,
    [DocDescription]	nvarchar(255) NULL,
    [Filename]      	nvarchar(255) NULL,
    [FileExtension] 	nvarchar(255) NULL,
    [FileSize]      	int NULL,
    [FileData]      	varbinary(max) NULL,
    [CreatedBy]     	nvarchar(255) NULL,
    [CreatedOn]     	datetime NULL DEFAULT (getdate()),
    [Active]        	bit NULL DEFAULT ((1)),
    CONSTRAINT [PK_WFD_ID] PRIMARY KEY CLUSTERED([WFD_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[WFDA_WorkflowData]  ( 
    [WFDA_ID]       	int IDENTITY(1,1) NOT NULL,
    [WF_ID]         	int NOT NULL,
    [Recommendation]	nvarchar(max) NOT NULL,
    CONSTRAINT [PK_WFDA] PRIMARY KEY CLUSTERED([WFDA_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[WFFI_WorkflowFacilityInformation]  ( 
    [WFFI_ID]                	int IDENTITY(1,1) NOT NULL,
    [WFCD_ID]                	int NOT NULL,
    [FacilityReference]      	nvarchar(1024) NULL,
    [RiskType]               	nvarchar(50) NOT NULL,
    [Tenor]                  	nvarchar(30) NOT NULL,
    [LocalInt]               	nvarchar(50) NOT NULL, -- no longer used
    [Currency]               	nvarchar(50) NOT NULL,
    [NetLimit]               	float NOT NULL,
    [FacilityDescription]    	nvarchar(1024) NULL,
    [CreditApplicationStatus]	nvarchar(50) NOT NULL,
    [RiskTaker]              	nvarchar(250) NOT NULL,
    [DealSpecific]           	bit NOT NULL DEFAULT ((1)),
    [ProposedRating]         	nvarchar(50) NULL,
    [Lgd]                    	float NOT NULL,
    [FacilityId]             	nvarchar(50) NOT NULL,
    [FacilityRating]         	nvarchar(30) NULL,
    [FacilityInvestmentType] 	nvarchar(250) NULL,
    [NetLimitZar]            	float NOT NULL,
    [LgdZar]                 	float NOT NULL,
    [RiskTakerGroupCode]        nvarchar(3) NULL,
    CONSTRAINT [PK_WFFI] PRIMARY KEY CLUSTERED([WFFI_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[WFQ_WorkflowQuorum]  ( 
    [WFQ_ID]                        	int IDENTITY(1,1) NOT NULL,
    [WF_ID]                         	int NOT NULL,
    [TotalNumberOfApprovalsRequired]	int NOT NULL,
    [Selected]                      	bit NOT NULL DEFAULT ((0)),
    CONSTRAINT [PK_WFQ] PRIMARY KEY CLUSTERED([WFQ_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[WFQD_WorkflowQuorumData]  ( 
    [WFQD_ID]    	int IDENTITY(1,1) NOT NULL,
    [WFQ_ID]     	int NOT NULL,
    [AFLMM_ID]   	int NOT NULL,
    [MinRequired]	int NOT NULL,
    [MaxRequired]	int NOT NULL,
    [Rank]       	int NOT NULL,
    CONSTRAINT [PK_WFQD] PRIMARY KEY CLUSTERED([WFQD_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[WIPRR_WipRulesResponse]  ( 
    [WIPRR_ID]         	int IDENTITY(1,1) NOT NULL,
    [WF_ID]            	int NOT NULL,
    [RulesResponseData]	varbinary(max) NULL,
    CONSTRAINT [PK_WIPRR] PRIMARY KEY CLUSTERED([WIPRR_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[WIPUR_WipUsersResponse]  ( 
    [WIPUR_ID]         	int IDENTITY(1,1) NOT NULL,
    [WF_ID]            	int NOT NULL,
    [UsersResponseData]	varbinary(max) NULL,
    CONSTRAINT [PK_WIPUR] PRIMARY KEY CLUSTERED([WIPUR_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

create table dbo.A_WFFL_WorkflowFundLimit
(
    WFFL_ID                      int                        not null,
    REV                   	int NOT NULL,
    REVTYPE                	tinyint NULL,
    REVEND		                 	int NULL,
    WFAPP_ID                     int                        not null,
    Counterparty_Limit           float                      not null,
    Converted_Equity_Limit       float                      not null,
    Goverment_Guarantee_Limit    float                      not null,
    Junior_Debt_Limit            float                      not null,
    Liquidation_Limit            float                      not null,
    Mezzanine_Debt_Limit         float                      not null,
    Preferred_Shares_Limit       float                      not null,
    Senior_Secured_Limit         float                      not null,
    Senior_Unsecured_Limit       float                      not null,
    Subordinated_Secured_Limit   float                      not null,
    Subordinated_Unsecured_Limit float                      not null,
    LDI_Fund                     float                      not null,
    FixedIncome_Fund             float                      not null,
    Credit_Fund                  float                      not null,
    MultiAsset_Fund              float                      not null,
    MultiManager_Fund            float                      not null,
    CreatedOn                    datetime default getdate() not null,
    CONSTRAINT [PK_A_WFFLT] PRIMARY KEY CLUSTERED([WFFL_ID],[REV])
        WITH FILLFACTOR = 80 ON [PRIMARY])
    ON [PRIMARY]
    WITH (
        DATA_COMPRESSION = NONE

        )
    go

create table WFFL_WorkflowFundLimit
(
    WFFL_ID                      int                        identity
        constraint PK_WFFL
        primary key
        with (fillfactor = 80),
    WFAPP_ID                     int                        not null,
    Counterparty_Limit           float                      not null,
    Converted_Equity_Limit       float                      not null,
    Goverment_Guarantee_Limit    float                      not null,
    Junior_Debt_Limit            float                      not null,
    Liquidation_Limit            float                      not null,
    Mezzanine_Debt_Limit         float                      not null,
    Preferred_Shares_Limit       float                      not null,
    Senior_Secured_Limit         float                      not null,
    Senior_Unsecured_Limit       float                      not null,
    Subordinated_Secured_Limit   float                      not null,
    Subordinated_Unsecured_Limit float                      not null,
    LDI_Fund                     float                      not null,
    FixedIncome_Fund             float                      not null,
    Credit_Fund                  float                      not null,
    MultiAsset_Fund              float                      not null,
    MultiManager_Fund            float                      not null,
    CreatedOn                    datetime default getdate() not null,
)
    go

-- ===== dailyMailTable.sql =====
CREATE TABLE [dbo].[MRDA_MailRequestsDailyAll]  (
	[MRDA_ID]                   int IDENTITY(1,1) NOT NULL,
	[SentTo]                   	nvarchar(255) NOT NULL,
	[CreatedOn]                	datetime NOT NULL DEFAULT (getdate()),
    [IsComplete]      	        bit NOT NULL DEFAULT ((1)),
    [CompletedOn]     	        datetime NULL,
    [LastTriedOn]     	        datetime NULL,
    [LastTriedMessage]	        nvarchar(max) NULL,
    [RetryCount]      	        int NULL,
	[ApproverAction_WF_IDs]  	nvarchar(255) NULL,
	[ApproverNoAction_WF_IDs]	nvarchar(255) NULL,
	[Capturer_WF_IDs]	        nvarchar(255) NULL,
	[MailModel]                	varbinary(max) NULL,
	[MailProcess]              	nvarchar(255) NULL,
	CONSTRAINT [PK_MRDA] PRIMARY KEY CLUSTERED([MRDA_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

-- ===== CA-3267.sql =====
alter table dbo.ER_ExternalRequests
    add IsLumos bit default 0 not null
    go

alter table dbo.WFAD_WorkflowApplicationData
    add IsLumos bit default 0 not null
    go

alter table dbo.WF_Workflow
    add WorkFlowSystemName varchar(10) default 'CASDM' not null
    go

alter table dbo.A_WFAD_WorkflowApplicationData
    add IsLumos bit default 0 not null
    go

alter table dbo.A_WF_Workflow
    add WorkFlowSystemName varchar(10) default 'CASDM' not null
    go

-- ===== constraints.sql =====
ALTER TABLE [dbo].[A_WF_Workflow]
    ADD CONSTRAINT [FK_A_WF_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_WF_Workflow]
    ADD CONSTRAINT [FK_A_WF_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_WFA_WorkflowApprovals]
    ADD CONSTRAINT [FK_A_WFA_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_WFA_WorkflowApprovals]
    ADD CONSTRAINT [FK_A_WFA_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_WFAD_WorkflowApplicationData]
    ADD CONSTRAINT [FK_A_WFAD_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_WFAD_WorkflowApplicationData]
    ADD CONSTRAINT [FK_A_WFAD_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_WFC_WorkflowChat]
    ADD CONSTRAINT [FK_A_WFC_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_WFC_WorkflowChat]
    ADD CONSTRAINT [FK_A_WFC_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_WFCD_WorkflowCounterpartyDetail]
    ADD CONSTRAINT [FK_A_WFCD_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_WFCD_WorkflowCounterpartyDetail]
    ADD CONSTRAINT [FK_A_WFCD_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_WFDA_WorkflowData]
    ADD CONSTRAINT [FK_A_WFDA_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_WFDA_WorkflowData]
    ADD CONSTRAINT [FK_A_WFDA_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_WFFI_WorkflowFacilityInformation]
    ADD CONSTRAINT [FK_A_WFFI_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_WFFI_WorkflowFacilityInformation]
    ADD CONSTRAINT [FK_A_WFFI_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_WFQ_WorkflowQuorum]
    ADD CONSTRAINT [FK_A_WFQ_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_WFQ_WorkflowQuorum]
    ADD CONSTRAINT [FK_A_WFQ_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_WFQD_WorkflowQuorumData]
    ADD CONSTRAINT [FK_A_WFQD_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_WFQD_WorkflowQuorumData]
    ADD CONSTRAINT [FK_A_WFQD_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[AFADG_ApprovalForumActiveDirectoryGroup]
    ADD CONSTRAINT [FK_AFADG_AFG]
   FOREIGN KEY([AFG_ID])
   REFERENCES [dbo].[AFG_ApprovalForumGroup]([AFG_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[AFG_ApprovalForumGroup]
    ADD CONSTRAINT [UC_AFG]
   UNIQUE ([GroupName])
   WITH (
      FILLFACTOR = 80,
      DATA_COMPRESSION = NONE
   ) ON [PRIMARY]
GO

ALTER TABLE [dbo].[DSR_DocStashRequests]
    ADD CONSTRAINT [FK_DSR_WF]
   FOREIGN KEY([WF_ID])
   REFERENCES [dbo].[WF_Workflow]([WF_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[ER_ExternalRequests]
    ADD CONSTRAINT [FK_ER_WF]
   FOREIGN KEY([WF_ID])
   REFERENCES [dbo].[WF_Workflow]([WF_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[MR_MailRequests]
    ADD CONSTRAINT [FK_MR_WF]
   FOREIGN KEY([WF_ID])
   REFERENCES [dbo].[WF_Workflow]([WF_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[RT_RequestType]
    ADD CONSTRAINT [UC_RT]
   UNIQUE ([Type])
   WITH (
      FILLFACTOR = 80,
      DATA_COMPRESSION = NONE
   ) ON [PRIMARY]
GO

ALTER TABLE [dbo].[WF_Workflow]
    ADD CONSTRAINT [FK_WF_WF]
   FOREIGN KEY([ReferencedWorkflowId])
   REFERENCES [dbo].[WF_Workflow]([WF_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[WF_Workflow]
    ADD CONSTRAINT [FK_WF_RT]
   FOREIGN KEY([RT_ID])
   REFERENCES [dbo].[RT_RequestType]([RT_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[WFA_WorkflowApprovals]
    ADD CONSTRAINT [FK_WFA_WF]
   FOREIGN KEY([WF_ID])
   REFERENCES [dbo].[WF_Workflow]([WF_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[WFA_WorkflowApprovals]
    ADD CONSTRAINT [FK_WFA_AFLMM]
   FOREIGN KEY([AFLMM_ID])
   REFERENCES [dbo].[AFLMM_ApprovalForumListManagementMapping]([AFLMM_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[WFAD_WorkflowApplicationData]
    ADD CONSTRAINT [UC_WFAD]
   UNIQUE ([WFCD_ID])
   WITH (
      FILLFACTOR = 80,
      DATA_COMPRESSION = NONE
   ) ON [PRIMARY]
GO
ALTER TABLE [dbo].[WFAD_WorkflowApplicationData]
    ADD CONSTRAINT [FK_WFAD_WFCD]
   FOREIGN KEY([WFCD_ID])
   REFERENCES [dbo].[WFCD_WorkflowCounterpartyDetail]([WFCD_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[WFC_WorkflowChat]
    ADD CONSTRAINT [FK_WFC_WF]
   FOREIGN KEY([WF_ID])
   REFERENCES [dbo].[WF_Workflow]([WF_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[WFCD_WorkflowCounterpartyDetail]
    ADD CONSTRAINT [FK_WFCD_WF]
   FOREIGN KEY([WF_ID])
   REFERENCES [dbo].[WF_Workflow]([WF_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[WFD_WorkflowDocument]
    ADD CONSTRAINT [FK_WFD_WF]
   FOREIGN KEY([WorkflowID])
   REFERENCES [dbo].[WF_Workflow]([WF_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[WFDA_WorkflowData]
    ADD CONSTRAINT [UC_WFDA]
   UNIQUE ([WF_ID])
   WITH (
      FILLFACTOR = 80,
      DATA_COMPRESSION = NONE
   ) ON [PRIMARY]
GO
ALTER TABLE [dbo].[WFDA_WorkflowData]
    ADD CONSTRAINT [FK_WFDA_WF]
   FOREIGN KEY([WF_ID])
   REFERENCES [dbo].[WF_Workflow]([WF_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[WFFI_WorkflowFacilityInformation]
    ADD CONSTRAINT [FK_WFFI_WFCD]
   FOREIGN KEY([WFCD_ID])
   REFERENCES [dbo].[WFCD_WorkflowCounterpartyDetail]([WFCD_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[WFQD_WorkflowQuorumData]
    ADD CONSTRAINT [FK_WFQD_WF]
   FOREIGN KEY([WFQ_ID])
   REFERENCES [dbo].[WFQ_WorkflowQuorum]([WFQ_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[WFQD_WorkflowQuorumData]
    ADD CONSTRAINT [FK_WFQD_AFLMM]
   FOREIGN KEY([AFLMM_ID])
   REFERENCES [dbo].[AFLMM_ApprovalForumListManagementMapping]([AFLMM_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[WIPRR_WipRulesResponse]
    ADD CONSTRAINT [FK_WIPRR_WF]
   FOREIGN KEY([WF_ID])
   REFERENCES [dbo].[WF_Workflow]([WF_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[WIPUR_WipUsersResponse]
    ADD CONSTRAINT [FK_WIPUR_WF]
   FOREIGN KEY([WF_ID])
   REFERENCES [dbo].[WF_Workflow]([WF_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

-- ===== sp_CreateUpdateActiveDirectoryUser.sql =====
CREATE PROCEDURE [dbo].[sp_CreateUpdateActiveDirectoryUser]
(
        @Alias nvarchar(100),
        @DisplayName nvarchar(100),
        @GivenName nvarchar(100),
        @Surname nvarchar(100),
        @Mail nvarchar(100)
)
as
begin
set nocount on

declare @ADU_ID int = 0
select @ADU_ID = isnull(ADU_ID, 0) from dbo.ADU_ActiveDirectoryUser
where
        [Alias] = @Alias

if(@ADU_ID = 0)
begin
        insert into dbo.ADU_ActiveDirectoryUser(Alias, DisplayName, GivenName, Surname, Mail)
        values(@Alias, @DisplayName, @GivenName, @Surname, @Mail)
end
else
begin
        update dbo.ADU_ActiveDirectoryUser
        set
                DisplayName = @DisplayName,
                GivenName = @GivenName,
                Surname = @Surname,
                Mail = @Mail,
                UpdatedOn = getdate()
        where
                ADU_ID = @ADU_ID
end

end
GO


-- ===== sp_GetActiveDirectoryUser.sql =====
CREATE PROCEDURE [dbo].[sp_GetActiveDirectoryUser]
(
@Username nvarchar(100)
)
as
begin
set nocount on

declare @ADU_ID int = 0

select @ADU_ID = isnull(ADU_ID, 0) from [dbo].[ADU_ActiveDirectoryUser]
where
        Alias = replace(@Username, 'RMB-NT\','')

if(@ADU_ID = 0)
begin
        select
                0 as ADU_ID,
                replace(@Username, 'RMB-NT\','') Alias,
                replace(@Username, 'RMB-NT\','') DisplayName,
                replace(@Username, 'RMB-NT\','') GivenName,
                replace(@Username, 'RMB-NT\','') Surname,
                'N/A' Mail
end
else
begin

        select
                ADU_ID,
                Alias,
                DisplayName,
                GivenName,
                Surname,
                case when len(Mail) = 0 then 'DLRMBBTCreditAnalytics@rmb.co.za' else Mail end as Mail
        from [dbo].[ADU_ActiveDirectoryUser]
        where
                Alias = replace(@Username, 'RMB-NT\','')
end

end
GO


-- ===== sp_DocumentArchive.sql =====
CREATE PROCEDURE [dbo].[sp_DocumentArchive]
(
	@DocumentId nvarchar(255)
)
as
begin
set nocount on

update dbo.WFD_WorkflowDocument
set
	Active = 0
where
	WFD_ID = cast(@DocumentId as int)

end
GO


-- ===== sp_DocumentDownload.sql =====
CREATE PROCEDURE [dbo].[sp_DocumentDownload]
(
	@DocumentId nvarchar(255)
)
as
begin
set nocount on

select
	a.WFD_ID as ASD_ID,
	a.[Filename],
	isnull(a.FileData, cast('' as varbinary(max))) as FileData,
	a.FileSize
from dbo.WFD_WorkflowDocument as a
where
	WFD_ID = cast(@DocumentId as int)

end
GO


-- ===== sp_DocumentList.sql =====
CREATE PROCEDURE [dbo].[sp_DocumentList]
(
	@WorkflowId nvarchar(255)
)
as
begin
set nocount on

select
	a.WFD_ID as ASD_ID,
	a.WorkflowID as A_ID,
	'' as DocKey,
	a.DocDescription as DocType,
	a.DocDescription,
	a.[Filename],
	a.FileExtension,
	a.FileSize,
	a.CreatedBy,
	a.CreatedOn,
	a.Active
from dbo.[WFD_WorkflowDocument] as a
where
	WorkflowID = @WorkflowId and
	Active = 1
order by
	a.WFD_ID

end
GO


-- ===== sp_DocumentUpload.sql =====
CREATE PROCEDURE [dbo].[sp_DocumentUpload]
(
	@WorkflowId nvarchar(255),
	@DocDescription nvarchar(255),
	@Filename nvarchar(255),
	@FileExtension nvarchar(255),
	@FileSize bigint,
	@FileData varbinary(max),
	@CreatedBy nvarchar(255)
)
as
begin
set nocount on

insert into [dbo].[WFD_WorkflowDocument] (WorkflowID, DocDescription, [Filename], FileExtension, FileSize, FileData, CreatedBy)
values(@WorkflowId, @DocDescription, @Filename, @FileExtension, @FileSize, @FileData, @CreatedBy)

select SCOPE_IDENTITY()
end
GO


-- ===== staticData.sql =====
INSERT INTO [P_Properties]([Name], [Value])
  VALUES('AmxErrorEmailTo', 'DLRMBBTCreditAnalytics@rmb.co.za')
GO
INSERT INTO [P_Properties]([Name], [Value])
  VALUES('AmxErrorEmailCc', '')
GO
INSERT INTO [P_Properties]([Name], [Value])
  VALUES('Environment', 'Prod')
GO
INSERT INTO [P_Properties]([Name], [Value])
  VALUES('AmxCasdmAppWsdlUrl', 'https://crsk-daf.rmb.co.za/approvalforum-soap/approvalforumexternal/ws/approvalForumExternal.wsdl')
GO
INSERT INTO [P_Properties]([Name], [Value])
  VALUES('AmxCasdmAppWsdlUsername', 'AmxCasdm')
GO
INSERT INTO [P_Properties]([Name], [Value])
  VALUES('AmxCasdmAppWsdlPassword', '')
GO
INSERT INTO [P_Properties]([Name], [Value])
  VALUES('AmxErrorEmailFrom ', 'amx@rmb.co.za')
GO
INSERT INTO [P_Properties]([Name], [Value])
  VALUES('AmxCasdmIntegrationEnabled', 'false')
GO

INSERT INTO [RT_RequestType]([Type])
  VALUES('Credit Application')
GO
INSERT INTO [RT_RequestType]([Type])
  VALUES('Memo')
GO

