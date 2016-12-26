import java.lang.System;
import java.net.*;
import java.util.*;
import java.io.*;

//
// A simple command line ftp client.
//

public class CSftp
{
	//Functionality for user input
	public static String getUsageMsg(String user_command)
	{
		String use = "\nUsage: ";
		switch (user_command) {
			case "close":
				return use + "close";
			case "open":
				return use + "open SERVER [PORT]";
			case "user":
				return use + "user USERNAME";
			case "quit":
				return use + "quit";
			case "dir":
				return use + "dir";
			case "pwd":
				return use + "pwd";
			case "cd":
				return use + "cd DIRECTORY";
			case "get":
				return use + "get REMOTE";
			case "put":
				return use + "put LOCAL";
			default:
				return "";
		}
	}


	static final int DEFAULT_PORT = 21;
	static final int SIZE_OF_BUFF = 4096;
	static final String PROMPT = "csftp> ";
	static final boolean PROCEED = false;
	static Scanner scan;
	static CSftp ftp;
	static CSftp.Connection Conn;
	static String Working_directory;
	static boolean Logged_In = false;

	public static void main(String [] args)
	{
		ftp = new CSftp();
		Conn = ftp.new Connection();
		scan = new Scanner(System.in);
		Working_directory = "";


		for (int len = 1; len > 0;) {
			boolean on =
					(Conn.sock.isConnected()) && (!Conn.sock.isClosed());
			print(PROMPT);

			String input = null;
			try {
				input = scan.nextLine();
			} catch (Exception e) {
				println(ErrorText("998"));
				System.exit(0);
			}

			if (input.isEmpty()) {
			} else {
				String[] EntireCommand = input.split(" ");
				String command = EntireCommand[0].toLowerCase().trim();
				int arguments = EntireCommand.length - 1;
				switch (command) {
					case "open":
						if (on) {
							println("Already session");
							break;
						}
						int port = DEFAULT_PORT;
						if (arguments == 2) {

						} else if (arguments == 1) {
						} else {
							println(ErrorText("901") + getUsageMsg(command));
							break;
						}
						Conn.init(EntireCommand[1], port);
						break;
					case "close":
						if (!on) {
							println("No Session to close");
							break;
						}
						if (arguments != 0) {
							println(ErrorText("901") + getUsageMsg(command));
							break;
						}
						Conn.close();
						break;
					case "quit":
						if (on)
							Conn.close();
						if (arguments != 0) {
							println(ErrorText("901") + getUsageMsg(command));
							break;
						}
						System.exit(0);
						break;
					case "user":
						if (!on) {
							println("No session, connect first");
							break;
						}
						if (arguments != 1) {
							println(ErrorText("901") + getUsageMsg(command));
							break;
						}
						sendUsr(EntireCommand[1]);
						break;
					case "dir":
						if (!on) {
							println("No session, connect first");
							break;
						}
						if (arguments != 0) {
							println(ErrorText("901") + getUsageMsg(command));
							break;
						}
						if (!Logged_In) {
							println("log in first");
							break;
						}
						listFiles();
						break;
					case "cd":
						if (!on) {
							println("Log in first");
							break;
						}
						if (arguments != 1) {
							println(ErrorText("901") + getUsageMsg(command));
							break;
						}
						cd(EntireCommand[1]);
						break;
					case "pwd":
						if (!on) {
							println("no password");
							break;
						}
						if (arguments != 0) {
							println(ErrorText("901") + getUsageMsg(command));
							break;
						}
						Password();
						if (Working_directory.length() > 0)
							println(Working_directory);
						break;

					case "get":
						if (!on) {
							println("No connection");
							break;
						}

						if (arguments != 1) {
							println(ErrorText("901") + getUsageMsg(command));
							break;
						}
						if (!Logged_In) {
							println("Not logged in");
							break;
						}
						getFile(EntireCommand[1]);
						break;
					case "put":
						if (!on) {

							println(ErrorText("901") + " No connection");
							break;
						}
						if (arguments != 1) {
							println(ErrorText("901") + getUsageMsg(command));
							break;
						}
						if (!Logged_In) {
							println("Not logged in.");
							break;
						}
						sendFile(EntireCommand[1]);
						break;
					default:

						println(ErrorText("900"));
						break;
				}
			}
		}
	}


	//Change current working directory
	//by sending a change working directory
	//and parsing the response
	public static void cd(String dir)
	{
		Conn.send("CWD " + dir);
		while (true) {
			String prefix = "";
			prefix = Conn.read();
			println(prefix);
			if (prefix.startsWith("250 ")) {
				Working_directory = Password();
				break;
			}
			if (prefix.startsWith("530 ")) {
				println(ErrorText("999") + " (need user login)");
				break;
			}
			if (prefix.startsWith("550 ")) {
				println(ErrorText("999") + " (no such directory)");
				break;
			}
		}
	}

	//get dir of current server by using the PWD
	//parses the response and uses it as current dir
	public static String Password()
	{
		String dir = "";
		Conn.send("PWD");
		while (true) {
			String prefix = "";
			prefix = Conn.read();
			if (prefix.startsWith("257 ")) {
				int q1 = prefix.indexOf('\"');
				int q2 = prefix.indexOf('\"', q1 + 1);
				if (q2 > 0)
					dir = prefix.substring(q1 + 1, q2);
				break;
			}
			if (prefix.startsWith("530 ")) {
				println(ErrorText("999") + " need user login");
				break;
			}
		}
		Working_directory = dir;
		return Working_directory;
	}

	//log in prompt for user, this will usually be anonymous
	//if server needs a password a 331 or 530 message will
	//be echoed and user will be prompted for password.
	public static void sendUsr(String usr)
	{
		Conn.send("USER " + usr);
		while (true) {
			String prefix = "";
			prefix = Conn.read();
			System.out.println(prefix);
			if (prefix.startsWith("331 ") || prefix.startsWith("530 Please ")) {
				print("PASSWORD: ");
				sendPw(scan.nextLine());
				break;
			}
			if (prefix.startsWith("530 Can't") || prefix.startsWith("500 ")) {
				println(ErrorText("999"));
				break;
			}
			if (prefix.startsWith("530 This ")) {
				println(ErrorText("999") + " (anonymous user)");
				break;
			}
			if (prefix.startsWith("230 ")) {
				System.out.println("asdfadfasd");
				Logged_In = true;
				break;
			}

		}
	}

	//sends password right after login,
	//except in the case of ftp servers that dont
	//accept passwords for anonymous users.
	public static void sendPw(String pw)
	{
		Conn.send("PASS " + pw);
		while (true) {
			String prefix = "";
			prefix = Conn.read();
			if (prefix.startsWith("230 ")) {
				Logged_In = true;
				println("Logged in.");
				break;
			}
			if (prefix.startsWith("530 Login ")) {

				println(ErrorText("999") + " login incorrect");
				break;
			}
			if (prefix.startsWith("503 Login ")) {
				println(ErrorText("999") + " login not correct");
				break;
			}
		}
	}

	//show all files and folders in current directory
	public static void listFiles()
	{
		String resp;
		Conn.send("PASV");

		 resp = Conn.read();
		println("Response from PASV " + resp);
		if (resp.startsWith("227 ")) {
			//if error, abandon
			Socket socket = null;
			String host = null;
			int port = 0;
			try {
				host = PSVhost(resp);
				port = getPSVport(resp);
				socket = new Socket(host, port);
			} catch(IOException e) {

				println(ErrorText("930") + " to " + host + " on port " + port  + " failed to open.");

			} catch(Exception e) {
				println(ErrorText("999") + " In passive mode");
			}
			try {
				Conn.send("LIST");
				String DirResponse = Conn.read();

				println(DirResponse);
				if (DirResponse.startsWith("150 ")) {

					BufferedReader buff_reader;

					try {
						String dir_element;

						boolean finder = false;

						Conn.read();
						buff_reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						while ((dir_element = buff_reader.readLine()) != null) {
							if (!finder) {
								finder = true;
							}println((dir_element));
						}
						buff_reader.close();

						socket.close();

						if (!finder) {
							println("Transfer Complete!!");
						}
					} catch (IOException e) {
						println(ErrorText("935"));
						socket.close();
					}
				}
			} catch(Exception e) {
				println(ErrorText("999") + "Directory");
			}
		}
		else {
			println(ErrorText("999") + " Failed PASV");
		}
	}

	//return host from fragments
	public static String PSVhost(String response) {
		String[] fragments = fragmentPSV(response);
		String host = fragments[0] + "." + fragments[1] + "." + fragments[2] + "." + fragments[3];
		return host;
	}

	//get port only
	public static int getPSVport(String resp)
	{
		String[] addressParts = fragmentPSV(resp);
		String p1 = addressParts[4];
		String p2 = addressParts[5];
		int port = (Integer.parseInt(p1) * 256) + Integer.parseInt(p2);
		return port;
	}


	//Uses the response from PSV and grabs the address.
	public static String[] fragmentPSV(String resp)
	{
		String address = resp.substring(resp.indexOf("(") + 1, resp.indexOf(")"));
		String[] add_part = address.split(",");
		return add_part;
	}

	//grab file from ftp in a dir
	public static void getFile(String file)
	{
		//transfer type 1 (binary)
		Conn.send("TYPE I");
		String typeResp = Conn.read();
		if (typeResp.startsWith("200 ")) {
			Conn.send("PASV");
			String pasvResp = Conn.read();
			if (pasvResp.startsWith("227 ")) {
				Socket socket = null;
				String host = null;
				int port = 0;
				try {
					host = PSVhost(pasvResp);
					port = getPSVport(pasvResp);
					socket = new Socket(host, port);

					if (PROCEED) {
						println("Getting: " + file +
								" to " + host + ":" + port);
					}

					Conn.send("RETR " + file);

					String Response = Conn.read();
					println(Response);
					if (Response.startsWith("150 ")) {
						InputStream in = socket.getInputStream();
						FileOutputStream out = new FileOutputStream(file);
						byte[] buffer = new byte[SIZE_OF_BUFF];
						int len;
						while ((len = in.read(buffer)) > 0)
						{
							out.write(buffer, 0, len);
						}
						in.close();
						out.close();
						socket.close();

						println(Conn.read());
					}
					else if (Response.startsWith("426 ")) {
					}
					else if (Response.startsWith("451 ") || Response.startsWith("551 ")) {
						println(ErrorText("910") + " " + file + " denied.");
					}
					else if (Response.startsWith("550 ")) {
						println(ErrorText("999") + " Failed to open file.");
					}

				} catch (IOException e) {
					println(ErrorText("930") +
							" to " + host + " on port " + port  + " failed to open.");
				} catch (Exception e) {
					println(ErrorText("999") + " get file");
				}
			}
			else {
				println(ErrorText("999") + " passive failed");
			}
		}
		else
		{
			println(ErrorText("999") + " transfer failed");
		}
	}



	//Save file to ftp server
	public static void sendFile(String file)
	{
		//Set the transfer type to binary
		Conn.send("TYPE I");

		String typeResp = Conn.read();
		if (typeResp.startsWith("200 ")) {
			Conn.send("PASV");
			String response = Conn.read();
			if (response.startsWith("227 ")) {
				Socket socket = null;
				String host = null;
				int port = 0;
				try {
					port = getPSVport(response);
					host = PSVhost(response);
					socket = new Socket(host, port);
					if (PROCEED) {
						println("Sending: " + file + " to " + host + ":" + port);
					}

					Conn.send("STOR " + file);

					String storResp = Conn.read();
					if (storResp.startsWith("150 ")) {
						OutputStream out = socket.getOutputStream();
						FileInputStream in = new FileInputStream(file);
						byte[] buf = new byte[SIZE_OF_BUFF];
						int length;
						while ((length = in.read(buf)) > 0) {
							out.write(buf, 0, length);
						}
						in.close();
						out.close();
						out.flush();
						socket.close();
						println(Conn.read());
					}
					else if (storResp.startsWith("426" )) {

					}
					else if ( storResp.startsWith("452")||   storResp.startsWith("451")|| storResp.startsWith("552")) {
						println(ErrorText("910") + " " + file + " cannot be accessed.");
					}
				} catch (IOException e) {
					println(ErrorText("930") +
							" to " + host + " on port " + port  + " did not open.");
				} catch (Exception e) {
					println(ErrorText("999") + " file");
				}
			}
			else {
				println(ErrorText("999") + " passive failed");
			}
		}
		else {
			println(ErrorText("999") + " transfer failed");
		}
	}

	private class Connection
	{
		private Socket sock;
		private String addr;
		private int port;
		private PrintWriter out;
		private BufferedReader in;

		public Connection()
		{
			sock = new Socket();
		}

		public void init(String address, int port)
		{

			this.port = port;
			this.addr = address;
			open();
		}

		private void open()
		{
			try {
				sock = new Socket(addr, port);
				out = new PrintWriter(
						sock.getOutputStream(), true);
				in = new BufferedReader(
						new InputStreamReader(sock.getInputStream()));

				println("Connected to " + sock.getRemoteSocketAddress() + ".");
			} catch(IOException e) {
				println(ErrorText("920") + " to " + addr + " on port " + port  + " did not open.");
			} catch(Exception e) {
				println(ErrorText("999") + " connection open");
			}
		}

		public void close()
		{
			try {
				in.close();
				out.close();
				sock.close();
				println("Connection closed. (" + addr + ":" + port + ")");
			} catch (Exception e) {
				println(ErrorText("999") + " connection close");
			}
		}

		public void send(String cmd)
		{
			try {
				if (PROCEED) {
					println("Send command: " + cmd);
				}
				println("--> " + cmd + "\r\n");
				out.write(cmd + "\r\n");
				out.flush();
			} catch (NullPointerException e) {
				println("No connection");
			} catch (Exception e) {
				println(ErrorText("925"));
				this.close();
			}
		}

		public String read() {
			String ln = "";

			try {
				ln = in.readLine();
			} catch (Exception e) {
				println(ErrorText("925"));
				this.close();
			}
			if (PROCEED) {
				if (!ln.startsWith("220 ")) {
					println(ln);
				}
			}
			return ln;
		}


		public String getAddress()
		{

			return this.addr;
		}
	}

	private static void println(Object o)
	{
		System.out.println(o);
	}

	private static void print(Object o)
	{
		System.out.print(o);
	}


	//Error messages based on error code.
	public static String ErrorText(String code)
	{
		switch (code) {
			case "900":
				return "900 Invalid Command.";
			case "901":
				return "901 Incorrect number of arguments.";
			case "910":
				return "910 Access to local file";
			case "920":
				return "920 Control connection";
			case "930":
				return "930 Data Transfer connection";
			case "925":
				return "925 Control connection I/O error, closing control connection.";
			case "935":
				return "935 Data transfer connection I/O error, closing data connection.";
			case "998":
				return "998 Input error while reading commands, terminating.";
			default:
				return "999 Processing error.";
		}
	}
}