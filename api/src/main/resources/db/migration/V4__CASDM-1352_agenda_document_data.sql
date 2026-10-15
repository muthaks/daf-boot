-- CASDM-1352: store the credit paper in AGID (docstash push deferred to agenda outputs).

ALTER TABLE [dbo].[AGID_AgendaItemDocument]
    ALTER COLUMN [DocStashId] nvarchar(255) NULL
GO

ALTER TABLE [dbo].[AGID_AgendaItemDocument]
    ADD [FileData] varbinary(max) NULL,
        [FileSize] bigint NULL,
        [FileExtension] nvarchar(20) NULL
GO

-- Audit table: file metadata is audited, file content is not (@NotAudited).
ALTER TABLE [dbo].[A_AGID_AgendaItemDocument]
    ADD [FileSize] bigint NULL,
        [FileExtension] nvarchar(20) NULL
GO
