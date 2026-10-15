-- CASDM-1352: document type per agenda document (Credit Paper, Annexure, ...).

ALTER TABLE [dbo].[AGID_AgendaItemDocument]
    ADD [DocType] nvarchar(100) NULL
GO

ALTER TABLE [dbo].[A_AGID_AgendaItemDocument]
    ADD [DocType] nvarchar(100) NULL
GO
