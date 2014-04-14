package com.ProjMgmtSys.model.Dept;
 
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;


public class DeptManager {
	private static Session session;
	
	private static void createSession(){
		session = SessionFactoryUtil.getSessionFactory().openSession();
        session.beginTransaction();
	}
	
	public static String createDept(String depName){
		Dept dept = new Dept();
		dept.setDepName(depName);
		
		createSession();
		session.save(dept);
		session.getTransaction().commit();
		session.close();
		return  "" + dept.getDepId();
	}
	
	
	@SuppressWarnings("unchecked")
	public static Dept queryDept(String depId){
		createSession();
		String hql = "from Dept as dept where dept.depId=:depId";
		Query query = session.createQuery(hql);
		query.setString("depId", depId);
		List <Dept>list = query.list();
		Dept dept = null;
		java.util.Iterator<Dept> iter = list.iterator();
		while (iter.hasNext()) {
			dept = iter.next();
		}					
		session.getTransaction().commit();
		session.close();
		return dept;		
	}
	
	public static void updateDepName(String depId, String depName){
		createSession();
		String hql1 = "update Dept as dept set dept.depName=:depName where depId=:depId";
		Query query1 = session.createQuery(hql1);
		query1.setString("depId", depId);
		query1.setString("depName", depName);
		query1.executeUpdate(); 
		session.getTransaction().commit();
		session.close();
	}
	
}