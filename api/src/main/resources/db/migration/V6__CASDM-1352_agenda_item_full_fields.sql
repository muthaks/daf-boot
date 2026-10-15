-- CASDM-1352: extend agenda items to cover the full "Call for Agenda Items" sheet
-- (scheduling, per-item committee roster, risk classification, distribution tracking, notes).

ALTER TABLE [dbo].[AGI_AgendaItem] ADD
    [StartTime]                 nvarchar(10)  NULL,
    [EndTime]                   nvarchar(10)  NULL,
    [RoundRobin]                bit           NOT NULL DEFAULT ((0)),
    [LecRep]                    nvarchar(255) NULL,
    [EqMemberFrmExec]           nvarchar(255) NULL,
    [BusinessRep1]              nvarchar(255) NULL,
    [SeniorBusinessRep]         nvarchar(255) NULL,
    [PortfolioCce1]             nvarchar(255) NULL,
    [PortfolioCce2]             nvarchar(255) NULL,
    [IndependentMember]         nvarchar(255) NULL,
    [Scribe]                    nvarchar(255) NULL,
    [MezzOverlayAttendees]      nvarchar(max) NULL,
    [EsgOverlayAttendees]       nvarchar(max) NULL,
    [AdditionalTeamAttendees]   nvarchar(max) NULL,
    [CountryOfRisk]             nvarchar(255) NULL,
    [IndustryClassification]    nvarchar(255) NULL,
    [LecCounterparty]           bit           NOT NULL DEFAULT ((0)),
    [DistributionSensitivities] nvarchar(max) NULL,
    [ApplicantRelationship]     nvarchar(50)  NULL,
    [ApplicationType]           nvarchar(100) NULL,
    [ProposalReceived]          bit           NOT NULL DEFAULT ((0)),
    [ProposalDistributedEmail]  bit           NOT NULL DEFAULT ((0)),
    [ProposalOnDiligent]        bit           NOT NULL DEFAULT ((0)),
    [Comments]                  nvarchar(max) NULL,
    [LateSubmissionReason]      nvarchar(max) NULL
GO

-- Audit table mirrors the same columns (all nullable, no defaults - Envers writes explicit values).
ALTER TABLE [dbo].[A_AGI_AgendaItem] ADD
    [StartTime]                 nvarchar(10)  NULL,
    [EndTime]                   nvarchar(10)  NULL,
    [RoundRobin]                bit           NULL,
    [LecRep]                    nvarchar(255) NULL,
    [EqMemberFrmExec]           nvarchar(255) NULL,
    [BusinessRep1]              nvarchar(255) NULL,
    [SeniorBusinessRep]         nvarchar(255) NULL,
    [PortfolioCce1]             nvarchar(255) NULL,
    [PortfolioCce2]             nvarchar(255) NULL,
    [IndependentMember]         nvarchar(255) NULL,
    [Scribe]                    nvarchar(255) NULL,
    [MezzOverlayAttendees]      nvarchar(max) NULL,
    [EsgOverlayAttendees]       nvarchar(max) NULL,
    [AdditionalTeamAttendees]   nvarchar(max) NULL,
    [CountryOfRisk]             nvarchar(255) NULL,
    [IndustryClassification]    nvarchar(255) NULL,
    [LecCounterparty]           bit           NULL,
    [DistributionSensitivities] nvarchar(max) NULL,
    [ApplicantRelationship]     nvarchar(50)  NULL,
    [ApplicationType]           nvarchar(100) NULL,
    [ProposalReceived]          bit           NULL,
    [ProposalDistributedEmail]  bit           NULL,
    [ProposalOnDiligent]        bit           NULL,
    [Comments]                  nvarchar(max) NULL,
    [LateSubmissionReason]      nvarchar(max) NULL
GO
