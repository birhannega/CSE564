<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>ASU Bank Profile and Settings</title>
<link rel="stylesheet" type="text/css" href="style.css" />
<script type="text/javascript" src="script.js"></script>
</head>
<body>
<div id="wrapper">
  <div id="header">
	  <div id="logo">
			<h1><a href="#">ASU Bank </a></h1>
		</div>
		<div id="slogan"> <ul>
			<li class="first current_page_item"><a href="employeeaccount.html">${employee}</a> </li>
			<li class="first current_page_item"><a href="#">Home</a> </li>
			<li><a href="#" id="strID"><%=session.getAttribute("strID") %></a></li>
			<li><a href="#">Services</a></li>
			<li><a href="logout.html">LogOut</a></li>
          </ul>		</div>
		
  	</div>
	<div id="menu">
			<ul>
				<li class="first current_page_item"><a href="account.html">Accounts</a></li>
				<li><a href="transfer.html">Transfers</a></li>
				<li><a href="profilesetting.html">Profile &amp; Settings</a></li>				
				<li><a href="#">Help &amp; Support</a></li>
			</ul>
	</div>
	<div style="padding:0">
		<div style="height:300px; width: 900px; padding:50px; margin:0px 20px; text-align:center">
			<div id="settings" style="height:300px;width:500px; float:center; margin:auto auto">								
				<table frame="box" rules="rows" style="border-spacing: 20px 25px;height:300px;margin:0">
					<tr>
						<td style="padding:0px 50px;">Accounts</td>
						<td>Balance</td>
					</tr>
					<tr>
						<td valign="middle"><a href="#" style="margin:0px 50px;">Checking (${checkingLastFour})</a></td>
						<td valign="middle">${checkingBalance}</td>
					</tr>
					<tr">
						<td valign="middle"><a href="#" style="margin:0px 50px;">Saving (${savingLastFour})</a></td>
						<td valign="middle">${savingBalance}</td>
					</tr>
					<tr">
						<td valign="middle"><a href="#" style="margin:0px 50px;">Credit (${creditLastFour})</a></td>
						<td valign="middle">${creditBalance}</td>
					</tr>
				</table>
				<!-- <a href="#">Change Password</a><br /><br />
				<a href="#">Change Contact</a><br /><br />
				<a href="#">Overdraft Protection</a>-->
			</div>			
		</div>
	</div>
		
	<div id="page-bottom">
		<div id="page-bottom-content">
			<h3>ASU Bank Policy</h3>
		  <p>Banking products are provided by Bank of America, N.A. and affiliated banks, Members FDIC and wholly owned subsidiaries of Bank of America Corporation.<br>
			  Investing in securities involves risks, and there is always the potential of losing money when you invest in securities. You should review any planned financial transactions that may have tax or legal implications with your personal tax or legal advisor.<br>
		  </p>
		</div>
		<div id="page-bottom-sidebar">
			<h3>Popular Links</h3>
			<ul class="list">
				<li class="first"><a href="#">Order checks</a></li>
				<li><a href="#">Order a Debit Card</a></li>
				<li class="last"><a href="#">Order a Foreign Currency</a></li>
			</ul>
		</div>
		<br class="clearfix" />
	</div>
</div>
<div id="footer">
	Copyright (c) 2013 ASUBank.com. All rights reserved.
</div>
</body>
</html>