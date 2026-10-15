package za.co.rmb.approvalforum.docgen.model;

public class PingModel {
    private boolean success;

    public PingModel() {

    }

    public PingModel(boolean success) {
        this.success = success;
    }

    public boolean getSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}