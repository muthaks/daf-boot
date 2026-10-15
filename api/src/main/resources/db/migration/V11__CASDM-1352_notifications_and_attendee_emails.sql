-- CASDM-1352: notification audit log (send status visible in the UI) and a place to hold the
-- resolved attendee e-mails on an item so the chair/attendees can be invited when the agenda
-- is compiled/locked.

CREATE TABLE [dbo].[AGN_AgendaNotification] (
    [AGN_ID]           int IDENTITY(1,1) NOT NULL,
    [AGI_ID]           int            NULL,
    [AGM_ID]           int            NULL,
    [NotificationType] nvarchar(50)   NULL,
    [Recipients]       nvarchar(2000) NULL,
    [Subject]          nvarchar(500)  NULL,
    [Status]           nvarchar(50)   NULL,
    [ErrorMessage]     nvarchar(2000) NULL,
    [SentOn]           datetime       NULL,
    CONSTRAINT [PK_AGN] PRIMARY KEY CLUSTERED([AGN_ID])
        WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
GO

ALTER TABLE [dbo].[AGI_AgendaItem] ADD
    [AttendeeEmails] nvarchar(max) NULL
GO

ALTER TABLE [dbo].[A_AGI_AgendaItem] ADD
    [AttendeeEmails] nvarchar(max) NULL
GO
