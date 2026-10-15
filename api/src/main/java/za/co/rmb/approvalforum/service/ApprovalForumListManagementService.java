package za.co.rmb.approvalforum.service;

import za.co.rmb.approvalforum.model.ListManagementList;
import za.co.rmb.approvalforum.model.UsersResponse;

import java.util.List;

public interface ApprovalForumListManagementService {

    ListManagementList getListManagementListByName(String name);

    UsersResponse getAllUsers();

    List<String> getAllListNames();

}

