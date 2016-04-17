package v1.a1;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class redditWebcrawlerGui extends JFrame implements ActionListener{
	public JTextArea usernameField;
	public JButton submitUsernameButton;
	public JPanel userInfoPanel;
	public redditWebcrawler Crawler;

	public static void main(String[] args) {
		// start the jframe
		redditWebcrawlerGui thisGui=new redditWebcrawlerGui();
		thisGui.setVisible(true);
	}
	public redditWebcrawlerGui(){
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.Crawler=new redditWebcrawler();
		
		this.setSize(400,400);
		this.setLayout(new FlowLayout());
		
		this.usernameField=new JTextArea(4,20);
		this.submitUsernameButton=new JButton("check");
		this.submitUsernameButton.addActionListener(this);

		
		this.userInfoPanel=new JPanel();
		userInfoPanel.setLayout(new BoxLayout(userInfoPanel,BoxLayout.X_AXIS));
		userInfoPanel.add(this.usernameField);
		userInfoPanel.add(this.submitUsernameButton);
		this.add(this.userInfoPanel);
	}
	public void actionPerformed(ActionEvent e) {
		Object thingPressed=e.getSource();
		if(thingPressed==this.submitUsernameButton){
			try {
				Crawler.startCrawl(this.usernameField.getText());
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		}
	}

}
