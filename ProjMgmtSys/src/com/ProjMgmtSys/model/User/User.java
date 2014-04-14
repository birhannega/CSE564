package com.ProjMgmtSys.model.User;

public class User {
	//Define the user type
	private String userId;
	private String userName;
	private String password;
	private int userType;
	private int groId;
	private int depId;
	private String groName;
	private String depName;
	private int fail = 0;
	private boolean block = false;
//	public User(){
//		fail = 0;
//		block = false;
//	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public int getUserType() {
		return userType;
	}
	public void setUserType(int userType) {
		this.userType = userType;
	}
	public int getGroId() {
		return groId;
	}
	public void setGroId(int groId) {
		this.groId = groId;
	}
	public int getDepId() {
		return depId;
	}
	public void setDepId(int depId) {
		this.depId = depId;
	}
	public int getFail() {
		return fail;
	}
	public void setFail(int fail) {
		this.fail = fail;
	}
	public boolean isBlock() {
		return block;
	}
	public void setBlock(boolean block) {
		this.block = block;
	}
	public String getGroName() {
		return groName;
	}
	public void setGroName(String groName) {
		this.groName = groName;
	}
	public String getDepName() {
		return depName;
	}
	public void setDepName(String depName) {
		this.depName = depName;
	}
}