package com.asubank.controller;

import com.asubank.model.transfer.Transaction;
import com.asubank.model.transfer.TransactionErrorCode;
import com.asubank.model.transfer.TransactionInput;
import com.asubank.model.transfer.TransactionManager;
import com.asubank.model.user.ContactSet;
import com.asubank.model.user.LoginResult;
import com.asubank.model.user.PasswordSet;
import com.asubank.model.user.SettingResultCode;
import com.asubank.model.user.UserManager;
import com.asubank.model.user.User;
import com.asubank.model.visitor.CapValidationRequestSource;
import com.asubank.model.visitor.Visitor;
import com.asubank.model.visitor.VisitorManager;
import com.asubank.model.account.Account;
import com.asubank.model.account.AccountManager;
import com.asubank.model.combinedcommand.UserInformation;
import com.asubank.model.combinedcommand.UserVisitor;
import com.asubank.model.pii.PartialPii;
import com.asubank.model.pii.Pii;
import com.asubank.model.pii.PiiManager;
import com.asubank.model.recipient.Recipient;
import com.asubank.model.recipient.RecipientInput;
import com.asubank.model.recipient.RecipientManager;
import com.asubank.model.security.ImagePath;
import com.asubank.model.security.Security;
import com.asubank.model.security.SecurityManager;
import com.asubank.model.security.StatusCode;
import com.asubank.model.sessionset.SessionSet;
import com.asubank.model.sessionset.SessionSetManager;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

@Controller("userController")
public class UserController {
	private static final String USERCAPTCHA = "usercaptcha";
	private static final String VISITORCAPTCHA = "visitorcaptcha";
	
	@RequestMapping("/login")
    public String login(@ModelAttribute("uservisitor") UserVisitor userVisitor, Model model, HttpSession session) throws IOException, InvalidKeyException, NoSuchAlgorithmException, ParseException {	
		userVisitor.setVisitor(VisitorManager.createVisitor());
//		String machineID = combinedCommand.getVisitor().getMachineID();
//		VisitorManager.createCaptcha(machineID);
//		String encodedImage = imageToByteArray(machineID, VISITORCAPTCHA);
//	    model.addAttribute("encodedImage",encodedImage); 
		
		session.setAttribute("machineID", userVisitor.getVisitor().getMachineID());
	    model.addAttribute("visitor", userVisitor.getVisitor());
        return "login";
    }
	
	@RequestMapping("/home")
	public String home(@RequestParam String action, @ModelAttribute("uservisitor") UserVisitor userVisitor, Model model, HttpSession session) throws InvalidKeyException, NoSuchAlgorithmException, ParseException, IOException{
		if(action.equals("Login")){
			String machineID = userVisitor.getVisitor().getMachineID();
			int captchaCode = VisitorManager.validateCaptcha(machineID, userVisitor.getVisitor().getCaptchaInput(), CapValidationRequestSource.LOGIN);
			if(captchaCode != StatusCode.CAPTCHA_VALIDATED && captchaCode != StatusCode.USERID_NOT_EXIST){
				String captchaStatus = StatusCode.CAPTCHA_STATUS[captchaCode];
				model.addAttribute("captchaStatus", captchaStatus);
				String encodedImage = imageToByteArray(machineID, VISITORCAPTCHA);
			    model.addAttribute("encodedImage",encodedImage); 
			    model.addAttribute("user", userVisitor.getUser());
			    model.addAttribute("visitor", userVisitor.getVisitor());
			    return "login";
			}
			//Validate login input format
			String LoginInputValidate = InputValidation.validateLogin(userVisitor.getUser().getStrID(), userVisitor.getUser().getPassword());
			if(LoginInputValidate != null){
				VisitorManager.increaseFail(machineID);
				VisitorManager.createCaptcha(machineID);
				String encodedImage = imageToByteArray(machineID, VISITORCAPTCHA);
			    model.addAttribute("encodedImage",encodedImage); 
				String ErrorMsg = LoginInputValidate;
				model.addAttribute("ErrorMsg", ErrorMsg);
				model.addAttribute("visitor", userVisitor.getVisitor());
				return "login";
			}
			//Match credential
			LoginResult loginResult = UserManager.validate(userVisitor.getUser().getStrID(), userVisitor.getUser().getPassword());
			
			if(loginResult.getUser() == null){
				VisitorManager.increaseFail(machineID);
				VisitorManager.createCaptcha(machineID);
				String encodedImage = imageToByteArray(machineID, VISITORCAPTCHA);
			    model.addAttribute("encodedImage",encodedImage); 
				String ErrorMsg = StatusCode.LOGIN_STATUS[loginResult.getStatusCode()];
				model.addAttribute("ErrorMsg", ErrorMsg);
				model.addAttribute("visitor", userVisitor.getVisitor());
				return "login";
			}	
			
			String strID = loginResult.getUser().getStrID();
			session.removeAttribute("machineID");
			session.setAttribute("strID", strID);
			String sessionID = session.getId();
			
			String sessionKey = (String)session.getAttribute("sessionKey");
			if(sessionKey == null){
				SessionSet sessionSet = SessionSetManager.createSessionSet(sessionID);
				sessionKey = sessionSet.getSessionKey();
				session.setAttribute("sessionKey", sessionKey);
			}
			else{
				SessionSet sessionSet = new SessionSet();
				sessionSet.setSessionID(sessionID);
				sessionSet.setSessionKey(sessionKey);
				if(SessionSetManager.validateSessionSet(sessionSet) == false){
					machineID = userVisitor.getVisitor().getMachineID();
					VisitorManager.createCaptcha(machineID);
					String encodedImage = imageToByteArray(machineID, VISITORCAPTCHA);
				    model.addAttribute("encodedImage",encodedImage); 
				    model.addAttribute("user", userVisitor.getUser());
				    model.addAttribute("visitor", userVisitor.getVisitor());
			        return "login";
				}
			}
			
			User user = loginResult.getUser();
			int roletype = user.getRoletype();
			if(roletype != 0){
				Account account = AccountManager.queryAccount(strID);
				String checkingID = String.valueOf(account.getCheckingID());
				String savingID = String.valueOf(account.getSavingID());
				String creditID = String.valueOf(account.getCreditID());			
				model.addAttribute("checkingLastFour", checkingID.substring(checkingID.length() - 4, checkingID.length()));
				model.addAttribute("savingLastFour", savingID.substring(savingID.length() - 4, savingID.length()));
				model.addAttribute("creditLastFour", creditID.substring(creditID.length() - 4, creditID.length()));
				model.addAttribute("checkingBalance", account.getCheckingBalance());
				model.addAttribute("savingBalance", account.getSavingBalance());
				model.addAttribute("creditBalance", account.getCreditBalance());
	//			Security security = SecurityManager.querySecurity(loginResult.getUser().getStrID());
	//			model.addAttribute("user",loginResult.getUser());
	//			model.addAttribute("security",security);
				VisitorManager.deleteVisitor(machineID);
				session.setMaxInactiveInterval(1200);
				return "account";
			}
			else{
				session.setMaxInactiveInterval(1200);
				return "employeeaccount";
			}
		}
		else{
			String machineID = userVisitor.getVisitor().getMachineID();
			VisitorManager.createCaptcha(machineID);
			String encodedImage = imageToByteArray(machineID, VISITORCAPTCHA);
		    model.addAttribute("encodedImage",encodedImage); 
		    model.addAttribute("user", userVisitor.getUser());
		    model.addAttribute("visitor", userVisitor.getVisitor());
	        return "login";
		}
	}
	
	@RequestMapping("/account")
	public String account(Model model, HttpSession session){
		Account account = AccountManager.queryAccount((String)session.getAttribute("strID"));
		if(account == null){
			return "sessionTimeOut";
		}
		String checkingID = String.valueOf(account.getCheckingID());
		String savingID = String.valueOf(account.getSavingID());
		String creditID = String.valueOf(account.getCreditID());	
		User user = UserManager.queryUser((String)session.getAttribute("strID"));
		if(user.getRoletype() == 0)
			model.addAttribute("employee", "Employee");
		model.addAttribute("checkingLastFour", checkingID.substring(checkingID.length() - 4, checkingID.length()));
		model.addAttribute("savingLastFour", savingID.substring(savingID.length() - 4, savingID.length()));
		model.addAttribute("creditLastFour", creditID.substring(creditID.length() - 4, creditID.length()));
		model.addAttribute("checkingBalance", account.getCheckingBalance());
		model.addAttribute("savingBalance", account.getSavingBalance());
		model.addAttribute("creditBalance", account.getCreditBalance());
		return "account";
	}
	
//	@RequestMapping("/customeraccount")
//	public String customerAccount(HttpSession session, Model model){
//		Account account = AccountManager.queryAccount((String)session.getAttribute("strID"));
//		String checkingID = String.valueOf(account.getCheckingID());
//		String savingID = String.valueOf(account.getSavingID());
//		String creditID = String.valueOf(account.getCreditID());	
//		model.addAttribute("employee", "Employee");
//		model.addAttribute("checkingLastFour", checkingID.substring(checkingID.length() - 4, checkingID.length()));
//		model.addAttribute("savingLastFour", savingID.substring(savingID.length() - 4, savingID.length()));
//		model.addAttribute("creditLastFour", creditID.substring(creditID.length() - 4, creditID.length()));
//		model.addAttribute("checkingBalance", account.getCheckingBalance());
//		model.addAttribute("savingBalance", account.getSavingBalance());
//		model.addAttribute("creditBalance", account.getCreditBalance());
//		return "account";
//	}
//	
	@RequestMapping("/merchant")
    public String merchant(Model model,HttpSession session){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
	String strID=(String)session.getAttribute("strID");
		
		return "merchant";
	}
	
	@RequestMapping("/Transfer")
	public String MakeTransfer(Model model,HttpSession session){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
		String strID=(String)session.getAttribute("strID");
		User user = UserManager.queryUser(strID);
		if(user.getRoletype() == 0)
			model.addAttribute("employee", "Employee");
			
		model.addAttribute("strID",strID);
//		Transaction transfer = new Transaction();
		
//		model.addAttribute("transfer", transfer);
		TransactionInput transactionInput = new TransactionInput();
		model.addAttribute("transferinput", transactionInput);
			return "MakeTransfer";
		}
	
	@RequestMapping("/MakeTransfer")
//	public String Transfer(@RequestParam String action,Model model,@ModelAttribute("transfer")Transaction transfer,HttpSession session) throws InvalidKeyException, NoSuchAlgorithmException, ParseException{
	public String Transfer(@RequestParam String action,Model model,@ModelAttribute("transferinput")TransactionInput transferInput,HttpSession session) throws InvalidKeyException, NoSuchAlgorithmException, ParseException{
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
		String strID=(String)session.getAttribute("strID");
		String message=null;
		model.addAttribute("strID",strID);
		User user = UserManager.queryUser(strID);
		if(user.getRoletype() == 0)
			model.addAttribute("employee", "Employee");
		//Validate Transfer input start
		String transferValidate = InputValidation.validateTransfer(transferInput);
		if(transferValidate != null){
			model.addAttribute("message", transferValidate);
			return "MakeTransfer";
		}
		Transaction transfer = new Transaction();
		transfer.setAmount(Double.valueOf(transferInput.getAmountInput()));
		transfer.setFromID(Long.valueOf(transferInput.getFromIDInput()));
		transfer.setToID(Long.valueOf(transferInput.getToIDInput()));
		//Validate Transfer input end
		TransactionManager transfermanager = new TransactionManager();
		if(action.equals("Continue")){
			Account account = AccountManager.queryAccount(strID);
			if(transfer.getFromID() != account.getCheckingID() && transfer.getFromID() != account.getSavingID()){
				model.addAttribute("message", TransactionErrorCode.NOTYOURACCOUNT);
				return "MakeTransfer";
			}
			double balance = getBalance(account, transfer.getFromID());
			if(balance < transfer.getAmount()){
				message = TransactionErrorCode.OVERDRAFT;
				model.addAttribute("message", message);
				return "MakeTransfer";
			}
			
			if(transfer.getAmount() >= 1000){
				Security security = SecurityManager.createOtp(strID);
				String otp = security.getOtp();
				SecurityManager.sendOTP(otp, user.getEmail());
				session.setAttribute("transfer", transfer);
				model.addAttribute("security", security);
				return "UserOtpValidation";
			}
			
			message = transfermanager.transferMoney(strID, transfer.getFromID(), transfer.getToID(), transfer.getAmount());
		}
		
		model.addAttribute("message", message);
		return "MakeTransfer";
	}
	
	@RequestMapping("/CheckingBalance")
	public String Checking(Model model,HttpSession session,Map<String, Object> map){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
		String strID=(String)session.getAttribute("strID");
		model.addAttribute("strID",strID);
		List transactions = TransactionManager.getTransactionsById(strID);

		map.put("transactions", transactions);
		User user = UserManager.queryUser(strID);
		if(user.getRoletype() == 0)
			model.addAttribute("employee", "Employee");
			return "CheckingBalance";
		}
	
	@RequestMapping("/RecipientInfo")
	public String RecipientInfo(Model model, HttpSession session){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
//		Recipient recipient = new Recipient();
		RecipientInput recipientInput = new RecipientInput();
//		model.addAttribute("recipient",recipient);
		model.addAttribute("recipientInput", recipientInput);
		User user = UserManager.queryUser((String)session.getAttribute("strID"));
		if(user.getRoletype() == 0)
			model.addAttribute("employee", "Employee");
		return "AddRecipient";
	}
	
	@RequestMapping(value="/AddRecipient")
//	public String AddRecipient(@RequestParam String action, @ModelAttribute("recipient") Recipient recipient ,Model model,HttpSession session){
	public String AddRecipient(@RequestParam String action, @ModelAttribute("recipientInput") RecipientInput recipientInput ,Model model,HttpSession session){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
		String strID=(String)session.getAttribute("strID");
		model.addAttribute("strID",strID);
		User user = UserManager.queryUser(strID);
		if(user.getRoletype() == 0)
			model.addAttribute("employee", "Employee");
		//Validate Recipient input start
		String recipientValidate = InputValidation.validateRecipient(recipientInput);
		if(recipientValidate != null){
			model.addAttribute("message", recipientValidate);
			return "AddRecipient";
		}
		Recipient recipient = new Recipient();
		recipient.setRecipient_lastname(recipientInput.getRecipient_lastnameInput());
		recipient.setRecipient_nickname(recipientInput.getRecipient_nicknameInput());
		recipient.setRecipient_accountnumber(Long.valueOf(recipientInput.getRecipient_accountnumberInput()));
		//Validate Recipient input end
//		String strID=(String)session.getAttribute("strID");
//		model.addAttribute("strID",strID);
		String message = null;
		if(action.equals("Verify"))
			{	
			RecipientManager rm=new RecipientManager();
			String result=	rm.verifyRecipient(strID, recipient.getRecipient_accountnumber());
			if(result.equals("1")){
				rm.addRecipient(strID, recipient.getRecipient_accountnumber(), recipient.getRecipient_lastname(),recipient.getRecipient_nickname());
			 message="Valid Recipient..Recipient Added to your list!!!";
			 }
			else{
			message="NO Account with such account number Exists in this BANK";
				}
			}
		
		model.addAttribute("message",message);
		return "AddRecipient";
		}
	
	@RequestMapping("/employeeaccount")
	public String employeeAccount(HttpSession session, Model model){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
		return "employeeaccount";
	}
	
//	@RequestMapping("/applynewaccount/{machineid}")
//    public String userInfo(@PathVariable(value="machineid") String machineID, Model model) throws InvalidKeyException, NoSuchAlgorithmException, ParseException, IOException {
//		User user = new User();
//        model.addAttribute("user", user);
	@RequestMapping("/applynewaccount")
	public String userInfo(Model model, HttpSession session) throws InvalidKeyException, NoSuchAlgorithmException, ParseException, IOException{
//		Visitor visitor = (Visitor) session.getAttribute("visitor");
//		String machineID = visitor.getMachineID();
		if((String)session.getAttribute("machineID") == null){
			return "sessionTimeOut";
		}
		String machineID = (String)session.getAttribute("machineID");
		VisitorManager.deleteVisitor(machineID);
		Visitor visitor = VisitorManager.createVisitor();	
		session.setAttribute("machineID", visitor.getMachineID());
//		Visitor visitor = VisitorManager.createVisitor();		
		machineID = visitor.getMachineID();
		VisitorManager.createCaptcha(machineID);
		String encodedImage = imageToByteArray(machineID, VISITORCAPTCHA);
	    model.addAttribute("encodedImage",encodedImage); 
		model.addAttribute("visitor",visitor);		
		UserInformation userinformation = new UserInformation();
		model.addAttribute("userinformation", userinformation);
        return "applynewaccount";
    }
	
	@RequestMapping("/forgetpwd")
	public String forgetPwd(Model model, HttpSession session){
		if((String)session.getAttribute("machineID") == null){
			return "sessionTimeOut";
		}
		String machineID = (String)session.getAttribute("machineID");
		if(machineID.equals("") == false)
			VisitorManager.deleteVisitor(machineID);
		model.addAttribute("user", new User());
		return "forgetpwd";
	}
	
	@RequestMapping("/sendpwd")
	public String sendPwd(@ModelAttribute("user") User user, Model model){
		String strID = user.getStrID();
		//validate strID start
		String strIDValidate = InputValidation.validateStrID(strID);
		if(strIDValidate != null){
			model.addAttribute("ErrorMsg", strIDValidate);
			model.addAttribute("machineid", "");
			return "forgetpwd";
		}
		//validate strID end		
		user = UserManager.queryUser(strID);
		if(user == null){
			String ErrorMsg = StatusCode.LOGIN_STATUS[StatusCode.USERID_NOT_EXIST];
			model.addAttribute("ErrorMsg", ErrorMsg);
			model.addAttribute("machineid", "");
			return "forgetpwd";
		}
		String tempPW = SecurityManager.createRandomPW();
		SecurityManager.updatePassword(strID, tempPW);
		SecurityManager.sendTempPw(tempPW, user.getEmail());
		String resultMessage = UserResultMessage.TEMP_PASSWORD_EMAIL_SENT;
		model.addAttribute("resultMessage", resultMessage);
		return "visitorresult";
	}		
//		String machineID = combinedCommand.getVisitor().getMachineID();
//		VisitorManager.createCaptcha(machineID);
//		String encodedImage = imageToByteArray(machineID, VISITORCAPTCHA);
//	    model.addAttribute("encodedImage",encodedImage); 
//	    model.addAttribute("visitor", combinedCommand.getVisitor());
		
//	}
	
	@RequestMapping("/createuser")
    public String createUser(@RequestParam String action, @ModelAttribute("userinformation") UserInformation userInformation, Model model, HttpSession session) throws InvalidKeyException, NoSuchAlgorithmException, ParseException, IOException {
		if(action.equals("Submit")){
			String machineID = userInformation.getVisitor().getMachineID();
			int captchaCode = VisitorManager.validateCaptcha(machineID, userInformation.getVisitor().getCaptchaInput(), CapValidationRequestSource.REGISTRATION);
			if(captchaCode != StatusCode.CAPTCHA_VALIDATED){
				String captchaStatus = StatusCode.CAPTCHA_STATUS[captchaCode];
				model.addAttribute("captchaStatus", captchaStatus);
				String encodedImage = imageToByteArray(machineID, VISITORCAPTCHA);
			    model.addAttribute("encodedImage",encodedImage); 
			    model.addAttribute("userInformation", userInformation);
			    model.addAttribute("visitor", userInformation.getVisitor());
			    return "applynewaccount";
			}	
			//validate userinfo start
			String userInfoValidate = InputValidation.validateUserInformation(userInformation);
			if(userInfoValidate != null){
				VisitorManager.createCaptcha(machineID);
				String encodedImage = imageToByteArray(machineID, VISITORCAPTCHA);
			    model.addAttribute("encodedImage",encodedImage); 
			    model.addAttribute("userInfoError", userInfoValidate);
				model.addAttribute("visitor", userInformation.getVisitor());
				return "applynewaccount";
			}
			//validate userinfo end
			int checkInfoCode = checkUserInfo(userInformation);
			if(checkInfoCode != UserInfoErrorCode.NO_ERROR){
				VisitorManager.createCaptcha(machineID);
				String encodedImage = imageToByteArray(machineID, VISITORCAPTCHA);
			    model.addAttribute("encodedImage",encodedImage); 
			    model.addAttribute("userInfoError", UserInfoErrorCode.USERINFOERROR[checkInfoCode]);
				model.addAttribute("visitor", userInformation.getVisitor());
				return "applynewaccount";
			}
			    
	        String otp = VisitorManager.createOtp(machineID, userInformation);
	        VisitorManager.sendOTP(otp, userInformation.getEmail());
	        session.setAttribute("userinformation", userInformation);
//	        model.addAttribute("userinformation", userInformation);
	        model.addAttribute("visitor", userInformation.getVisitor());
	        return "otpvalidation";
//	        String resultMessage = UserResultMessage.NEW_ACCOUNT_APPLICATION_ACCEPTED;
//	    	model.addAttribute("resultMessage", resultMessage);
////	    	VisitorManager.deleteVisitor(machineID);
//	        return "visitorresult";
		}
		else{
			String machineID = userInformation.getVisitor().getMachineID();
			VisitorManager.createCaptcha(machineID);
			String encodedImage = imageToByteArray(machineID, VISITORCAPTCHA);
		    model.addAttribute("encodedImage",encodedImage); 
		    model.addAttribute("visitor", userInformation.getVisitor());
	        return "applynewaccount";
		}
    }
	
	private int checkUserInfo(UserInformation userInfo){
		int year = Integer.valueOf(userInfo.getDobYear());
		int month = Integer.valueOf(userInfo.getDobMonth());
		int day = Integer.valueOf(userInfo.getDobDay());
//		int year = userInfo.getDobYear();
//		int month = userInfo.getDobMonth();
//		int day = userInfo.getDobDay();
		String pwd1 = userInfo.getPassword();
		String pwd2 = userInfo.getPwdConfirm();
		String transpwd1 = userInfo.getTransPwd();
		String transpwd2 = userInfo.getTransPwdConfirm();
		if(checkDate(year, month, day) != true){
			return UserInfoErrorCode.INVALID_DOB;
		}
		if(pwd1.equals(pwd2) != true){
			return UserInfoErrorCode.ACCOUNT_PASSWORD_NOT_CONFIRMED;
		}
		if(transpwd1.equals(transpwd2) != true){
			return UserInfoErrorCode.TRANSACTION_PASSWORD_NOT_CONFIRMED;
		}
		if(pwd1.equals(transpwd1)){
			return UserInfoErrorCode.SAME_ACCOUNT_TRANSACTION_PASSWORD;
		}
		if(checkPassword(pwd1) == false){
			return UserInfoErrorCode.SIMPLE_ACCOUNT_PASSWORD;
		}
		if(checkPassword(transpwd1) == false){
			return UserInfoErrorCode.SIMPLE_TRANSACTION_PASSWORD;
		}
		
//		char c;
//		for(int i = 0; i < pwd1.length(); i++){
//			c = pwd1.charAt(i);
//			if (c >= 'a' && c <= 'z'){
//				lowercaseAcct = true;
//			}
//			else if (c >= 'A' && c <= 'Z'){
//				uppercaseAcct = true;
//			}
//			else if (c >= '0' && c <= '9'){
//				numberAcct = true;
//			}			
//		}
//		if(lowercaseAcct == false || uppercaseAcct == false || numberAcct == false){
//			return UserInfoErrorCode.SIMPLE_ACCOUNT_PASSWORD;
//		}
//		for(int i = 0; i < transpwd1.length(); i++){
//			c = transpwd1.charAt(i);
//			if (c >= 'a' && c <= 'z'){
//				lowercaseTrans = true;
//			}
//			else if (c >= 'A' && c <= 'Z'){
//				uppercaseTrans = true;
//			}
//			else if (c >= '0' && c <= '9'){
//				numberTrans = true;
//			}			
//		}
//		if(lowercaseTrans == false || uppercaseTrans == false || numberTrans == false){
//			return UserInfoErrorCode.SIMPLE_TRANSACTION_PASSWORD;
//		}
		return UserInfoErrorCode.NO_ERROR;
	}
	
	private static boolean checkPassword(String password){
		boolean uppercasePw = false;
		boolean lowercasePw =false;
		boolean numberPw = false;
		char c;
		for(int i = 0; i < password.length(); i++){
			c = password.charAt(i);
			if (c >= 'a' && c <= 'z'){
				uppercasePw = true;
			}
			else if (c >= 'A' && c <= 'Z'){
				lowercasePw = true;
			}
			else if (c >= '0' && c <= '9'){
				numberPw = true;
			}			
		}
		if(uppercasePw == false || lowercasePw == false || numberPw == false){
			return false;
		}
		return true;		
	}
	
	@RequestMapping("/profilesetting")
	public String profilesetting(@ModelAttribute("user") User user, Model model, HttpSession session){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
		model.addAttribute("user",user);
		String strID = (String)session.getAttribute("strID");
		User user0 = UserManager.queryUser(strID);
		if(user0.getRoletype() == 0)
			model.addAttribute("employee", "Employee");
		return "profilesetting";
	}
	
	@RequestMapping("/checkingid")
	public String getUserCheckingID(HttpSession session, Model model){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
		String strID = (String)session.getAttribute("strID");
		String str = "";
		Account account = AccountManager.queryAccount(strID);
		str = String.valueOf(account.getCheckingID());
		model.addAttribute("checkingID", str);
		model.addAttribute("user",new User());
		User user = UserManager.queryUser(strID);
		if(user.getRoletype() == 0)
			model.addAttribute("employee", "Employee");
		return "profilesetting";
	}
	
	@RequestMapping("/savingid")
	public String getUserSavingID(HttpSession session, Model model){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
		String strID = (String)session.getAttribute("strID");
		String str = "";
		Account account = AccountManager.queryAccount(strID);
		str = String.valueOf(account.getSavingID());
		model.addAttribute("savingID", str);
		model.addAttribute("user",new User());
		User user = UserManager.queryUser(strID);
		if(user.getRoletype() == 0)
			model.addAttribute("employee", "Employee");
		return "profilesetting";
	}
	
	@RequestMapping("/creditid")
	public String getUserCreditID(HttpSession session, Model model){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
		String strID = (String)session.getAttribute("strID");
		String str = "";
		Account account = AccountManager.queryAccount(strID);
		str = String.valueOf(account.getCreditID());
		model.addAttribute("creditID", str);
		model.addAttribute("user",new User());
		User user = UserManager.queryUser(strID);
		if(user.getRoletype() == 0)
			model.addAttribute("employee", "Employee");
		return "profilesetting";
	}
	
	@RequestMapping("/email")
	public String getUserEmail(HttpSession session, Model model){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
		String strID = (String)session.getAttribute("strID");
		User user = UserManager.queryUser(strID);
		model.addAttribute("email", user.getEmail());
		model.addAttribute("user",new User());
		if(user.getRoletype() == 0)
			model.addAttribute("employee", "Employee");
		return "profilesetting";
	}
	
	@RequestMapping("/address")
	public String getUserAddress(HttpSession session, Model model){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
		String strID = (String)session.getAttribute("strID");
		User user = UserManager.queryUser(strID);
		model.addAttribute("address", user.getAddress());
		model.addAttribute("user",new User());
		if(user.getRoletype() == 0)
			model.addAttribute("employee", "Employee");
		return "profilesetting";
	}
	
	@RequestMapping("/telephone")
	public String getUserTelephone(HttpSession session, Model model){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
		String strID = (String)session.getAttribute("strID");
		User user = UserManager.queryUser(strID);
		model.addAttribute("telephone", user.getTelephone());
		model.addAttribute("user",new User());
		if(user.getRoletype() == 0)
			model.addAttribute("employee", "Employee");
		return "profilesetting";
	}
		
//	@RequestMapping("/profilesetting/email")
//	public @ResponseBody String getEmail(HttpSession session, Model model){
//		String str = "";
//		User user = UserManager.queryUser(strID);
//		str = "{\"email\":\"" + user.getEmail() + "\"}";		
//		return str;
//	}
//	
//	@RequestMapping("/profilesetting/acctno")
//	public @ResponseBody String getAcctno(@RequestParam(value = "strID") String strID){
//		String str = "";
//		Account account = AccountManager.queryAccount(strID);
//		str = "{\"acctno\":\"" + account.getSavingID() + "\"}";		
//		return str;
//	}
//	
//	@RequestMapping("/profilesetting/address")
//	public @ResponseBody String getAddress(@RequestParam(value = "strID") String strID){
//		String str = "";
//		User user = UserManager.queryUser(strID);
//		str = "{\"address\":\"" + user.getAddress() + "\"}";
//		return str;
//	}
//	
//	@RequestMapping("/profilesetting/phone")
//	public @ResponseBody String getProfile(@RequestParam(value = "strID") String strID){
//		String str = "";
//		User user = UserManager.queryUser(strID);
//		str = "{\"phone\":\"" + user.getTelephone() + "\"}";
//		return str;
//	}
	
//	@RequestMapping("/profilesetting/{item}")
//	public @ResponseBody String getProfile(@RequestParam(value = "strID") String strID,
//										   @RequestParam(value = "item") String item){
//		String str = "";
//		System.out.println("a");
//		if(item.equals("acctno")){
//			Account account = AccountManager.queryAccount(strID);
//			str =  "{\"result\":\"" + account.getSavingID() + "\"}";
//		}
//		else{
//			User user = UserManager.queryUser(strID);
//			if(item.equals("email")){
//				str = "{\"result\":\"" + user.getEmail() + "\"}";
//			}
//			else if(item.equals("address")){
//				str = "{\"result\":\"" + user.getAddress() + "\"}";
//			}
//			else{
//				str = "{\"result\":\"" + user.getTelephone() + "\"}";
//			}
//			
//		}
//		System.out.println("STR: " + str);
//		return str;
//}
	
//	@RequestMapping("/userlist")
//    public ModelAndView userList(Model model) {
//		List<User> list = UserManager.queryAllUsers();
//        model.addAttribute("message", list);
//        return new ModelAndView("userlist", "list", list);
//    }
	
	@RequestMapping("/createotp")
	public String createOTP(HttpSession session, Model model) throws InvalidKeyException, NoSuchAlgorithmException, ParseException{
		if((String)session.getAttribute("machineID") == null){
			return "sessionTimeOut";
		}
		String machineID = (String)session.getAttribute("machineID");
		Visitor visitor = VisitorManager.queryVisitor(machineID);	
		UserInformation userinformation = (UserInformation)session.getAttribute("userinformation");
		String otp = VisitorManager.createOtp(machineID, userinformation);
		VisitorManager.sendOTP(otp, userinformation.getEmail());
		model.addAttribute("visitor",visitor);
		return "otpvalidation";		
	}
	
	@RequestMapping("/validateotp")
	public String validateOTP(@ModelAttribute("visitor") Visitor visitor, HttpSession session, Model model){
		if((String)session.getAttribute("machineID") == null){
			return "sessionTimeOut";
		}
		String machineID = (String)session.getAttribute("machineID");
		UserInformation userinformation = (UserInformation)session.getAttribute("userinformation");
		int statusCode = VisitorManager.validateOtp(machineID, visitor.getOtpInput());	
		if(statusCode == StatusCode.OTP_VALIDATED){
			String userID = createUserAccount(userinformation);		
			String resultMessage = UserResultMessage.NEW_ACCOUNT_CREATED;
			VisitorManager.sendNewAccountInfo(userID, userinformation.getPassword(), userinformation.getTransPwd(), userinformation.getEmail());
	    	model.addAttribute("resultMessage", resultMessage);
	    	VisitorManager.deleteVisitor(machineID);
	    	session.removeAttribute("machineID");
	    	session.removeAttribute("userinformation");
	        return "visitorresult";
		}
		String status = StatusCode.STATUS[statusCode];
		model.addAttribute("status", status);
		model.addAttribute("visitor",visitor);
		return "otpvalidation";
	}
	
	@RequestMapping("/usercreateotp")
	public String createUserOTP(HttpSession session, Model model) throws InvalidKeyException, NoSuchAlgorithmException, ParseException{
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
		String strID = (String)session.getAttribute("strID");
		User user = UserManager.queryUser(strID);	
		Security security = SecurityManager.createOtp(strID);
		String otp = security.getOtp();
		SecurityManager.sendOTP(otp, user.getEmail());
		model.addAttribute("security",security);
		return "UserOtpValidation";		
	}
	
	@RequestMapping("/uservalidateotp")
	public String validateOTP(@ModelAttribute("security") Security security, HttpSession session, Model model){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
		String strID = (String)session.getAttribute("strID");
		int statusCode = SecurityManager.validateOtp(strID, security.getOtpInput());	
		if(statusCode == StatusCode.OTP_VALIDATED){
			Transaction transfer = (Transaction)session.getAttribute("transfer");
			TransactionManager transactionManager = new TransactionManager();
			String message = transactionManager.transferMoney(strID, transfer.getFromID(), transfer.getToID(), transfer.getAmount());
	    	model.addAttribute("resultMessage", message);
	    	session.removeAttribute("transfer");
	        return "userresult";
		}
		String status = StatusCode.STATUS[statusCode];
		model.addAttribute("status", status);
		model.addAttribute("security",security);
		return "UserOtpValidation";
	}
	
	private static String createUserAccount(UserInformation userInformation){
		User user = new User();		
		Pii pii = new Pii();
		PartialPii partialPii = new PartialPii();
		
		user.setFirstname(userInformation.getFirstname());
		user.setLastname(userInformation.getLastname());
		user.setAddress(userInformation.getAddress());
		user.setEmail(userInformation.getEmail());
		user.setTelephone(userInformation.getTelephone());
		user.setRoletype(userInformation.getRoletype());
		user.setPassword(userInformation.getPassword());
		String message = "";    	
	    message = UserManager.createUser(user.getFirstname(), user.getLastname(), user.getAddress(), user.getEmail(), user.getTelephone(), user.getRoletype(),
	    			user.getPassword());
	    Security security = new Security(message);
		security.setTransPwd(userInformation.getTransPwd());
		
		pii.setSsn(userInformation.getSsn());
		pii.setDobYear(Integer.valueOf(userInformation.getDobYear()));
		pii.setDobMonth(Integer.valueOf(userInformation.getDobMonth()));
		pii.setDobDay(Integer.valueOf(userInformation.getDobDay()));
//		pii.setDobYear(userInformation.getDobYear());
//		pii.setDobMonth(userInformation.getDobMonth());
//		pii.setDobDay(userInformation.getDobDay());
		String ssnString = pii.getSsn();
		String ssnLastFour = ssnString.substring(ssnString.length() - 4, ssnString.length());
		partialPii.setDobYear(pii.getDobYear());
		partialPii.setSsnLastFour(ssnLastFour);

    	pii.setStrID(message);
    	partialPii.setStrID(message);
        SecurityManager.createSecurity(security);
        PiiManager.createPii(pii);
        PiiManager.createPartialPii(partialPii);
        AccountManager.createAccount(message);
        return message;
	}
	
/*otp related functions for user, not visitor
	@RequestMapping("/createotp")
	public String createOTP(@ModelAttribute("user") User user, Model model) throws InvalidKeyException, NoSuchAlgorithmException, ParseException{
		Security security = SecurityManager.createOtp(user.getStrID());
		model.addAttribute("user",user);
		model.addAttribute("security", security);
		return "home";		
	}
	
	@RequestMapping("/validateotp")
	public String validateOTP(@ModelAttribute("security") Security security, Model model){
		int statusCode = SecurityManager.validateOtp(security.getStrID(), security.getOtpInput());
		String status = StatusCode.STATUS[statusCode];
		model.addAttribute("status", status);
		User user = UserManager.queryUser(security.getStrID());
		model.addAttribute("user",user);
		return "home";
	}
*/	
	@RequestMapping("/createcaptcha")
	public String createCaptcha(@ModelAttribute("user") User user, Model model) throws InvalidKeyException, NoSuchAlgorithmException, ParseException, IOException{
		Security security = SecurityManager.createCaptcha(user.getStrID());
		String encodedImage = imageToByteArray(user.getStrID(), USERCAPTCHA);
	    model.addAttribute("encodedImage",encodedImage);
		model.addAttribute("user",user);
		model.addAttribute("security", security);
		return "home";		
	}
	
	@RequestMapping("/validatecaptcha")
	public String validateCaptcha(@ModelAttribute("security") Security security, Model model) throws InvalidKeyException, NoSuchAlgorithmException, ParseException, IOException{
		int statusCode = SecurityManager.validateCaptcha(security.getStrID(), security.getCaptchaInput());
		String statusCap = StatusCode.CAPTCHA_STATUS[statusCode];
		
		if(statusCode != 0){
			String encodedImage = imageToByteArray(security.getStrID(), USERCAPTCHA);
		    model.addAttribute("encodedImage",encodedImage);
		}
		
		model.addAttribute("statusCap", statusCap);
		User user = UserManager.queryUser(security.getStrID());
		model.addAttribute("user",user);
		return "home";
	}
	
	@RequestMapping("/setting")
	public String setting(@RequestParam String action, @ModelAttribute("user") User user, Model model, HttpSession session){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
		model.addAttribute("user", user);
		User user0 = UserManager.queryUser((String)session.getAttribute("strID"));
		if(user0.getRoletype() == 0)
			model.addAttribute("employee", "Employee");
		if(action.equals("Change Password")){
			PasswordSet passwordSet = new PasswordSet();
			model.addAttribute("passwordset",passwordSet);
			return "changepassword";
		}
		else{
			ContactSet contactSet = new ContactSet();
			model.addAttribute("contactset",contactSet);
			return "changecontact";
		}
	}
	
	@RequestMapping("/updatepassword")
	public String updatePassword(@ModelAttribute("passwordset") PasswordSet passwordSet, Model model, HttpSession session){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
//		if(action.equals("Update Password")){	
		User user0 = UserManager.queryUser((String)session.getAttribute("strID"));
		if(user0.getRoletype() == 0)
			model.addAttribute("employee", "Employee");
		//updatepassword input validate start
		String passwordSetValidate = InputValidation.validatePasswordSet(passwordSet);
		if(passwordSetValidate != null){
			User user = new User();
			user.setStrID(passwordSet.getStrID());
			passwordSet = new PasswordSet();
			model.addAttribute("passwordset",passwordSet);
			model.addAttribute("ErrorMsg", InputErrorCode.PASSWORD_ERROR);
			model.addAttribute("user",user);
			return "changepassword";
		}
		//updatepassword input validate end
		if(passwordSet.getOldPassword().equals(passwordSet.getNewPassword())){
			User user = new User();
			user.setStrID(passwordSet.getStrID());
			passwordSet = new PasswordSet();
			model.addAttribute("passwordset",passwordSet);
			model.addAttribute("ErrorMsg", SettingResultCode.SAME_OLD_AND_NEW_PASSWORD);
			model.addAttribute("user",user);
			return "changepassword";
		}
		else if(passwordSet.getConfirmPassword().equals(passwordSet.getNewPassword()) != true){
			User user = new User();
			user.setStrID(passwordSet.getStrID());
			passwordSet = new PasswordSet();
			model.addAttribute("passwordset",passwordSet);
			model.addAttribute("ErrorMsg", SettingResultCode.PASSWORD_NOT_CONFIRMED);
			model.addAttribute("user",user);
			return "changepassword";
		}
		else if(checkPassword(passwordSet.getNewPassword()) == false){
			User user = new User();
			user.setStrID(passwordSet.getStrID());
			passwordSet = new PasswordSet();
			model.addAttribute("passwordset",passwordSet);
			model.addAttribute("ErrorMsg", UserInfoErrorCode.USERINFOERROR[UserInfoErrorCode.SIMPLE_ACCOUNT_PASSWORD]);
			model.addAttribute("user",user);
			return "changepassword";
		}
		else{
			User user = UserManager.queryUser(passwordSet.getStrID());
			String originalPassword = user.getPassword();
			if(originalPassword.equals(passwordSet.getOldPassword()) != true){
				user = new User();
				user.setStrID(passwordSet.getStrID());
				passwordSet = new PasswordSet();
				model.addAttribute("passwordset",passwordSet);
				model.addAttribute("ErrorMsg", SettingResultCode.PASSWORD_NOT_VALIDATED);
				model.addAttribute("user",user);
				return "changepassword";
			}
			else{
				Security security = SecurityManager.querySecurity(passwordSet.getStrID());
				if(security.getTransPwd().equals(passwordSet.getNewPassword())){
					user = new User();
					user.setStrID(passwordSet.getStrID());
					passwordSet = new PasswordSet();
					model.addAttribute("passwordset",passwordSet);
					model.addAttribute("ErrorMsg", SettingResultCode.SAME_TRANS_AND_NEW_PASSWORD);
					model.addAttribute("user",user);
					return "changepassword";
				}					
				UserManager.updatePassword(passwordSet.getStrID(), passwordSet.getNewPassword());
				user = new User();
				user.setStrID(passwordSet.getStrID());
				model.addAttribute("user",user);
				model.addAttribute("resultMessage", SettingResultCode.PASSWORD_UPDATED);
				return "userresult";
			}
		}
//		}
//		else{
//			User user = new User();
//			user.setStrID(passwordSet.getStrID());
//			model.addAttribute("user",user);
//			return "profilesetting";
//		}
	}
	
	@RequestMapping("/updatecontact")
	public String updateContact(@ModelAttribute("contactset") ContactSet contactSet, Model model, HttpSession session){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
		User user0 = UserManager.queryUser((String)session.getAttribute("strID"));
		if(user0.getRoletype() == 0)
			model.addAttribute("employee", "Employee");
		User user = new User();		
		user.setStrID(contactSet.getStrID());
		//updatepassword input validate start
		String contactSetValidate = InputValidation.validateContactSet(contactSet);
		if(contactSetValidate != null){
			user.setStrID(contactSet.getStrID());
			contactSet = new ContactSet();
			model.addAttribute("contactSet",contactSet);
			model.addAttribute("ErrorMsg", contactSetValidate);
			model.addAttribute("user",user);
			return "changecontact";
		}
		//updatepassword input validate end
		
		int validate = UserManager.validatePassword(user.getStrID(), contactSet.getPassword());
		if(validate != StatusCode.LOGIN_SUCCESS){
			model.addAttribute("user",user);
			contactSet = new ContactSet();
			model.addAttribute("contactset",contactSet);
			model.addAttribute("ErrorMsg",SettingResultCode.PASSWORD_NOT_VALIDATED);
			return "changecontact";
		}		
		UserManager.updateContact(contactSet);		
		model.addAttribute("user",user);
		model.addAttribute("resultMessage", SettingResultCode.CONTACT_UPDATED);
		return "userresult";		
	}
	
	@RequestMapping("/transfer")
	public String transfer(Model model, HttpSession session){
		if((String)session.getAttribute("strID") == null){
			return "sessionTimeOut";
		}
//		model.addAttribute("user",user);
		return "transfer";
	}
	
	@RequestMapping("/logout")
	public String logout(HttpSession session, HttpServletRequest request, Model model){
		Visitor visitor = VisitorManager.createVisitor();
		session.invalidate();
		session = request.getSession();
		session.setAttribute("machineID", visitor.getMachineID());
		UserVisitor uservisitor = new UserVisitor();
		uservisitor.setUser(new User());
		uservisitor.setVisitor(visitor);
	    model.addAttribute("visitor", visitor);
	    model.addAttribute("uservisitor", uservisitor);
        return "login";
	}
	
	@RequestMapping("/errorhandling")
	public String errorhandling(HttpSession session, HttpServletRequest request, Model model){
		Visitor visitor = VisitorManager.createVisitor();
		session.invalidate();
		session = request.getSession();
		session.setAttribute("machineID", visitor.getMachineID());
		UserVisitor uservisitor = new UserVisitor();
		uservisitor.setUser(new User());
		uservisitor.setVisitor(visitor);
	    model.addAttribute("visitor", visitor);
	    model.addAttribute("uservisitor", uservisitor);
        return "login";
	}
	
	private static double getBalance(Account account, long accountID){
		if(account.getCheckingID() == accountID){
			return account.getCheckingBalance();
		}
		else
			return account.getSavingBalance();
	}
	
	//From: http://mrbool.com/how-to-convert-image-to-byte-array-and-byte-array-to-image-in-java/25136#
	private static String imageToByteArray(String strID, String type) throws IOException{
		BufferedImage image = ImageIO.read(new File(ImagePath.BACKGROUND + type + "\\" + strID + ".png"));
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ImageIO.write(image, "png", baos);
//	    byte[] res=baos.toByteArray();
	    String encodedImage = Base64.encode(baos.toByteArray());
	    return encodedImage;
	}
	
	private static boolean checkDate(int dobYear, int dobMonth, int dobDay){
		if(((dobMonth == 4 || dobMonth == 6 || dobMonth == 9 || dobMonth == 11) && dobDay == 31) || 
		   (dobMonth == 2 && dobDay > 29)){			
			return false;
		}
		if(dobMonth == 2 && dobDay == 29){
			if((dobYear % 4 == 0 && dobYear % 100 != 0) || (dobYear % 400 == 0)){
				return true;
			}
			else
				return false;
		}
		return true;
	}
	
	
//	private static void main(String[] args){
//		 
//		InetAddress ip;
//		try {
//	 
//			ip = InetAddress.getLocalHost();
//			System.out.println("Current IP address : " + ip.getHostAddress());
//	 
//			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
//	 
//			byte[] mac = network.getHardwareAddress();
//	 
//			System.out.print("Current MAC address : ");
//	 
//			StringBuilder sb = new StringBuilder();
//			for (int i = 0; i < mac.length; i++) {
//				sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));		
//			}
//			System.out.println(sb.toString());
//	 
//		} catch (UnknownHostException e) {
//	 
//			e.printStackTrace();
//	 
//		} catch (SocketException e){
//	 
//			e.printStackTrace();
//	 
//		}
//	 
//	   }
	@RequestMapping("/hello")
	 public @ResponseBody
	 String hello(@RequestParam(value = "name") String name,
	   @RequestParam(value = "gender") String gender,
	   @RequestParam(value = "email") String email,
	   @RequestParam(value = "phone") String phone,
	   @RequestParam(value = "city") String city) {
	  System.out.println(name);
	  System.out.println(gender);
	  System.out.println(email);
	  System.out.println(phone);
	  System.out.println(city);

	  String str = "{\"user\": { \"name\": \"" + name + "\",\"gender\": \""
	    + gender + "\",\"email\": \"" + email + "\",\"phone\": \""
	    + phone + "\",\"city\": \"" + city + "\"}}";
	  return str;

	 }
}
