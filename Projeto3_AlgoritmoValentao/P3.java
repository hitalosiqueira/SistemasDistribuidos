import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/*Criado por Hitalo Siqueira 408476 */
/*Criado por Marcos Cavalcante 408336 */

public class P3 extends Thread {
    private static int nProcessos = 4;  
    private static final String PID = "3";
    private static final String OK = "ok";
    private static final String CAIU = "caiu";
    private static final String ELEICAO = "eleicao";
    private static final String SUBIU = "subiu";
    private static final String parametroDefault = "default";
    private static final String COORDENADOR = "corrdenador";
    private static ServerSocket server;
    private Socket p3Cliente;
    private static ReentrantLock lock = new ReentrantLock();
    private boolean lider = false;

    public P3( Socket p3Cliente ) {
        
        this.p3Cliente = p3Cliente;
    }

    public static void criaServer() throws IOException {
        server = new ServerSocket(8003);
    }

    public static void iniciaServer() {
        lock.lock();
        new Thread() {
            public void run() {
                try{
                    while(true){
                        Socket listener = server.accept();
                        P3 processo3 = new P3(listener);
                        processo3.start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        lock.unlock();
    }

    public static void iniciaCliente( String tipoMensagem, String destino){
        lock.lock();
        new Thread(){
            public void run(){
                
                try {
                    if (tipoMensagem.equals(ELEICAO)){
                        if(8000 + nProcessos >= 8004){
                            Socket processo4 = new Socket("localhost", 8004);
                            BufferedWriter escreveProcesso4 = new BufferedWriter(new OutputStreamWriter(processo4.getOutputStream()));
                            escreveProcesso4.write(ELEICAO + "?" + PID);
                            escreveProcesso4.newLine();
                            escreveProcesso4.flush();
                            processo4.close();
                        }                     
                    }else{
                        if(tipoMensagem.equals(COORDENADOR)){
                            Socket processo1 = new Socket("localhost", 8001);
                            BufferedWriter escreveProcesso1 = new BufferedWriter(new OutputStreamWriter(processo1.getOutputStream()));
                            Socket processo2 = new Socket("localhost", 8002);
                            BufferedWriter escreveProcesso2 = new BufferedWriter(new OutputStreamWriter(processo2.getOutputStream()));

                            escreveProcesso1.write(COORDENADOR + "?" + PID);
                            escreveProcesso1.newLine();
                            escreveProcesso1.flush();

                            escreveProcesso2.write(COORDENADOR + "?" + PID);
                            escreveProcesso2.newLine();
                            escreveProcesso2.flush();

                            processo1.close();
                            processo2.close();
                        }else{
                            if(tipoMensagem.equals(OK)){
                                String porta = "800" + destino;
                                Socket processo = new Socket("localhost", Integer.parseInt(porta));
                                BufferedWriter escreveProcesso = new BufferedWriter(new OutputStreamWriter(processo.getOutputStream()));
                                escreveProcesso.write(OK + "?" + PID);
                                escreveProcesso.newLine();
                                escreveProcesso.flush();   
                                processo.close();
                            }                      
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        lock.unlock();
    }

    public void run() {
        try{
            BufferedReader entradaProcesso3 = new BufferedReader(new InputStreamReader(p3Cliente.getInputStream()));
            String entrada = entradaProcesso3.readLine();

            do{
                StringTokenizer token = new StringTokenizer(entrada, "?");

                String tipoMensagem = token.nextToken();
                String origem = token.nextToken();
                
                if(tipoMensagem.equals(OK)){
                    System.out.println("Processo " + origem + " é maior que eu. Fico quieto");
                }else{
                    if(tipoMensagem.equals(ELEICAO)){
                    
                        System.out.println("Processo " + origem + " quer ser coordenador");
                    
                        if(Integer.parseInt(PID) > Integer.parseInt(origem)){
                            System.out.println("Quieto processo " + origem + ". Eu sou maior.");
                            iniciaCliente(OK, origem);

                            if(nProcessos >= Integer.parseInt(PID)){
                                int n = Integer.parseInt(PID) + 1;
                                System.out.println("\tEleicao de " + PID + " para " + n);
                                iniciaCliente(ELEICAO, parametroDefault);
                            }
                            if(nProcessos == Integer.parseInt(PID)){
                                System.out.println("Eu sou o coordenador!");
                                iniciaCliente(COORDENADOR, parametroDefault);
                            }               
                        }
                    }else{
                        if(tipoMensagem.equals(COORDENADOR)){
                            System.out.println("O processo " + origem + " é o coordenador"); 
                        }else{
                            if(tipoMensagem.equals(CAIU)){
                                System.out.println("\n\nCoordenador caiu\n\n");
                                nProcessos--;
                                iniciaCliente(ELEICAO, parametroDefault);
                            }
                        }
                    }
                }
                entrada = entradaProcesso3.readLine();
            }while(entrada != null);
        }catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public static void main (String[] args) throws IOException {
        criaServer();
        iniciaServer();
        /*Scanner terminalInput = new Scanner(System.in);
        int op;
        
        do{
            System.out.println("1 - Eleicao");
            System.out.println("2 - Cair");
            op = terminalInput.nextInt();

            switch (op){
                case 1:
                    iniciaCliente(ELEICAO, parametroDefault);
                break;
                case 2:
                    iniciaCliente(CAIU, parametroDefault);
                break;
                case 3:
                    iniciaCliente(SUBIU, parametroDefault);
                break;
                default:
                    System.out.println("MESSAGE UNKNOWN");
                break;
            }

            try {
                Thread.sleep(17000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }while (true);*/
        
    }
}
