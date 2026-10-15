package za.co.rmb.approvalforum.web.resources;

import jakarta.inject.Inject;
import org.springframework.transaction.annotation.Transactional;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.apache.commons.io.IOUtils;
import za.co.rmb.credit.daf.jaxrs.multipart.InputPart;
import za.co.rmb.credit.daf.jaxrs.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.model.ApplicationDocumentModel;
import za.co.rmb.approvalforum.model.UploadDocForm;
import za.co.rmb.approvalforum.service.ApprovalForumDocumentsService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static za.co.rmb.approvalforum.security.util.SecurityHelper.tokenValidation;

@Api(tags = "documents")
@Path("/documents/")
@org.springframework.stereotype.Component
@org.springframework.context.annotation.Scope("prototype")
public class ApprovalForumDocumentsResource {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumDocumentsResource.class);

    @Context
    private SecurityContext context;

    @Inject
    private ApprovalForumDocumentsService approvalForumDocumentsService;

    @GET
    @Path("/list")
    @Produces("application/json")
    @ApiOperation(value = "Get list by workflow id",
            notes = "",
            authorizations = @Authorization(value = "Bearer"))
    public Response getList(@QueryParam("workflowId") String workflowId) {
        logger.info("getList()");

        return Response.ok(approvalForumDocumentsService.getWorkflowDocuments(workflowId)).build();
    }

    @POST
    @Path("/upload")
    @Produces("application/json")
    @Consumes("multipart/form-data")
    @Transactional
    @ApiOperation(value = "Upload document for workflow id",
            notes = "",
            authorizations = @Authorization(value = "Bearer"))
    public Response upload(MultipartFormDataInput form) {
        logger.info("upload()");

        tokenValidation(context);

        UploadDocForm newForm = new UploadDocForm();
        String fileName = "";

        Map<String, List<InputPart>> uploadForm = form.getFormDataMap();
        List<InputPart> inputParts = uploadForm.get("file");

        for (InputPart inputPart : inputParts) {
            try {
                MultivaluedMap<String, String> header = inputPart.getHeaders();
                fileName = getFileName(header);

                // convert the uploaded file to inputstream
                InputStream inputStream = inputPart.getBody(InputStream.class, null);

                byte[] bytes = IOUtils.toByteArray(inputStream);

                newForm.setFilename(fileName);
                newForm.setFileSize(bytes.length);
                newForm.setFiledata(bytes);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        try {
            String appID = uploadForm.get("appID").get(0).getBody(String.class, null);
            newForm.setAppID(appID);

            String createdBy = uploadForm.get("createdBy").get(0).getBody(String.class, null);
            newForm.setCreatedBy(createdBy);

            String docType = uploadForm.get("docType").get(0).getBody(String.class, null);
            newForm.setDocType(docType);

            String docDescription = uploadForm.get("docDescription").get(0).getBody(String.class, null);

            newForm.setDocDescription(docType);            

            String fileExtension = FindExtension(fileName);
            newForm.setFileExtension(fileExtension);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Response.ok(approvalForumDocumentsService.uploadDocument(newForm)).build();
    }

    @GET
    @Path("/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = "Download document by document id",
            notes = "",
            authorizations = @Authorization(value = "Bearer"))
    public Response download(@QueryParam("documentId") String documentId) {
        logger.info("download()");

        tokenValidation(context);

        ApplicationDocumentModel result = approvalForumDocumentsService.downloadDocument(documentId);

        StreamingOutput stream = output -> output.write(result.getFileData());

        return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
                .header("content-disposition", "attachment; filename=\"" + result.getFileName() + "")
                .header("Content-Length", result.getFileData().length).build();
    }

    @GET
    @Path("/archive")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Archive document by document id",
            notes = "",
            authorizations = @Authorization(value = "Bearer"))
    public Response archiveDoc(@QueryParam("documentId") String documentId) {
        logger.info("archiveDoc()");

        tokenValidation(context);

        return Response.ok(approvalForumDocumentsService.archiveDocument(documentId)).build();
    }


    private String getFileName(MultivaluedMap<String, String> header) {

        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");

        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {

                String[] name = filename.split("=");

                String finalFileName = name[1].trim().replaceAll("\"", "");
                return finalFileName;
            }
        }
        return "unknown";
    }

    private String FindExtension(String filename) {
        int indexOfDot = filename.length();
        for (int i = filename.length() - 1; i >= 0; i--) {
            char c = filename.charAt(i);
            if (c == '.') {
                indexOfDot = i;
                break;
            }
            if (c >= '0' && c <= '9')
                continue;
            if (c >= 'A' && c <= 'Z')
                continue;
            if (c >= 'a' && c <= 'z')
                continue;
            break;
        }
        // The dot must be followed by at least one other character,
        // so if the last character is a dot, return the empty string
        return indexOfDot + 1 == filename.length() ? "" : filename.substring(indexOfDot);
    }

}
