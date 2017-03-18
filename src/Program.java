import messenger.ProgramController;

public final class Program {
	
	public static void main(String args[]) {
		
		String serverName;
		int port;
		
		if(args.length == 2) {
			serverName = args[1];
			port = Integer.parseInt(args[2]);
		}
		else{
//			serverName = "147.188.195.121";

			serverName = "localhost";
			port = 54321;
		}
		
		new ProgramController(serverName, port);
		
	}
	
}