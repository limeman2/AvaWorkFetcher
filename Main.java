import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class Main {
	private static String FILE_NAME = "jobs.txt";
	private static String EMAIL_AUTH = "";
	private static String EMAIL_FROM = "";
	private static String EMAIL_TO = "";
	
	
	public static void main(String[] args) {
		if(args.length != 3) {
			System.exit(0);
		}
		
		EMAIL_AUTH = args[0];
		EMAIL_FROM = args[1];
		EMAIL_TO = args[2];
		
		try {
			
			// Get job opportunities
			
			Elements foundJobs = getRemoteJobs();
			List<String> foundJobsStrings = new ArrayList<String>();
			for(Element e : foundJobs) {
				foundJobsStrings.add(e.html());
			}
			List<String> storedJobs = getStoredJobs();
			
			// Compare lists
			
			for(Element e : foundJobs) {			
				if(!storedJobs.contains(e.html())) {
					sendEmail("New job opportunity at Avanza bank!",
							"<a href=\" " +
							"https://ext.workbuster.se/" + e.attr("href") +
							"\">Link</a>" +
							Jsoup.connect("https://ext.workbuster.se/" + e.attr("href")).get().toString());
					storedJobs.add(e.html());
				}
			}
			
			Iterator<String> i = storedJobs.iterator();
			
			while(i.hasNext()) {
				String e = i.next();

				if(!foundJobsStrings.contains(e)) {
					sendEmail("Job opportunity \"" + e + "\" is no longer available at Avanza Bank.", "");
					i.remove();
				}
			}
			
			// Save updated list of jobs
			
			Path f = Paths.get(FILE_NAME);
			Files.write(f, storedJobs, Charset.forName("UTF-8"));
		} catch (IOException e) {
		    sendEmail("Error while running AvaJobFetcher", e.getMessage() + "<br>" + e.getStackTrace());
		}
				
	}
	
	private static void sendEmail(String subject, String html) {
		final String username = EMAIL_FROM;
        final String password = EMAIL_AUTH;

        Properties props = new Properties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
          new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
          });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(EMAIL_TO));
            message.setSubject(subject);
            message.setContent(html, "text/html; charset=utf-8");

            Transport.send(message);

        } catch (MessagingException e) {
	    sendEmail("Error while running AvaJobFetcher", e.getMessage() + "<br>" + e.getStackTrace());
        }
	}
	
	private static Elements getRemoteJobs() throws IOException {
		Document doc = Jsoup.connect("https://ext.workbuster.se/index.php?site=avanza").get();
		return doc.select("div.item_text_container").select("a[href]:not(.readMore)");
	}
	
	private static List<String> getStoredJobs() throws FileNotFoundException {
		File file = new File(FILE_NAME);
		Scanner scan = new Scanner(file);
		List<String> storedJobs = new ArrayList<String>();
		
		while(scan.hasNextLine()) {
			storedJobs.add(scan.nextLine());
		}
		scan.close();
		return storedJobs;
	}
}
