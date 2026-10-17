-- A quorum exception is asked for by the analyst and decided by the committee's secretary or chair,
-- separately from submission. Status: Requested, Approved or Declined.
ALTER TABLE [dbo].[AGI_AgendaItem] ADD
    [QuorumExceptionStatus]       nvarchar(20)   NULL,
    [QuorumExceptionReason]       nvarchar(1000) NULL,
    [QuorumExceptionRequestedBy]  nvarchar(255)  NULL,
    [QuorumExceptionRequestedOn]  datetime       NULL,
    [QuorumExceptionDecidedBy]    nvarchar(255)  NULL,
    [QuorumExceptionDecidedOn]    datetime       NULL,
    [QuorumExceptionDecisionNote] nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ADD
    [QuorumExceptionStatus]       nvarchar(20)   NULL,
    [QuorumExceptionReason]       nvarchar(1000) NULL,
    [QuorumExceptionRequestedBy]  nvarchar(255)  NULL,
    [QuorumExceptionRequestedOn]  datetime       NULL,
    [QuorumExceptionDecidedBy]    nvarchar(255)  NULL,
    [QuorumExceptionDecidedOn]    datetime       NULL,
    [QuorumExceptionDecisionNote] nvarchar(1000) NULL
GO
