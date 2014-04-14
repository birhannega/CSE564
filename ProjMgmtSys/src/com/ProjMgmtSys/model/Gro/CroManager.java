package com.ProjMgmtSys.model.Gro;
 
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;


public class CroManager {
	private static Session session;
	
	private static void createSession(){
		session = SessionFactoryUtil.getSessionFactory().openSession();
        session.beginTransaction();
	}
	
	public static String createGro(String groName, String depId){
		Gro gro = new Gro();
		gro.setGroName(groName);
		gro.setDepId(depId);
		
		createSession();
		session.save(gro);
		session.getTransaction().commit();
		session.close();
		return  "" + gro.getGroId();
	}
	
	
	@SuppressWarnings("unchecked")
	public static Gro queryGro(String groId){
		createSession();
		String hql = "from Gro as gro where gro.groId=:groId";
		Query query = session.createQuery(hql);
		query.setString("groId", groId);
		List <Gro>list = query.list();
		Gro gro = null;
		java.util.Iterator<Gro> iter = list.iterator();
		while (iter.hasNext()) {
			gro = iter.next();
		}					
		session.getTransaction().commit();
		session.close();
		return gro;	
	}
	
	public static void updateGroName(String groId, String groName){
		createSession();
		String hql1 = "update Gro as gro set gro.groName=:groName where groId=:groId";
		Query query1 = session.createQuery(hql1);
		query1.setString("groId", groId);
		query1.setString("groName", groName);
		query1.executeUpdate(); 
		session.getTransaction().commit();
		session.close();
	}
	
}