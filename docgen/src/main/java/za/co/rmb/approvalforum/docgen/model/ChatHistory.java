package za.co.rmb.approvalforum.docgen.model;

public class ChatHistory {

    private String date;
    private String name;
    private String comment;

    public ChatHistory() { }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return this.comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}