-- Agenda Builder: bookings saved part-way through the booking wizard, to continue later from any PC.
-- A draft reserves no slot and is not an agenda item; Payload is the wizard's state as JSON.
CREATE TABLE [dbo].[AGBD_AgendaBookingDraft]  (
    [AGBD_ID]          	int IDENTITY(1,1) NOT NULL,
    [OwnerId]          	nvarchar(255) NOT NULL,
    [AGC_ID]           	int NULL,
    [MeetingDate]      	datetime NULL,
    [CounterpartyName] 	nvarchar(500) NULL,
    [Step]             	int NOT NULL DEFAULT ((0)),
    [Payload]          	nvarchar(max) NOT NULL,
    [CreatedBy]        	nvarchar(255) NOT NULL,
    [CreatedOn]        	datetime NOT NULL DEFAULT (getdate()),
    [UpdatedOn]        	datetime NULL,
    [Active]           	bit NOT NULL DEFAULT ((1)),
    CONSTRAINT [PK_AGBD] PRIMARY KEY CLUSTERED([AGBD_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO
CREATE NONCLUSTERED INDEX [IX_AGBD_OwnerId] ON [dbo].[AGBD_AgendaBookingDraft]([OwnerId], [Active])
GO
