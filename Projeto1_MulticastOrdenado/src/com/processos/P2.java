import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/*Criado por Hitalo Siqueira */

public class P2 extends Thread {

    private static TreeMap<String, String> filaMensagem = new TreeMap<String, String>();
    private static TreeMap<String, Integer> filaAck = new TreeMap<String, Integer>();
    private static final String PID = "2";
    private static AtomicInteger timestamp = new AtomicInteger(0);
    private static ServerSocket server;
    private Socket p2Cliente;
    private static ReentrantLock lock = new ReentrantLock();

    public P2( Socket p2Cliente ) {
        this.p2Cliente = p2Cliente;
    }

    public static void criaServer() throws IOException {
        server = new ServerSocket(8002);
    }

    public static void iniciaServer() {
        lock.lock();
        new Thread() {
            public void run() {
                try{
                    while(true){
                        Socket listener = server.accept();
                        P2 processo2 = new P2(listener);
                        processo2.start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        lock.unlock();
    }

    public static void iniciaCliente( String tipoMensagem ){
        lock.lock();
        new Thread(){
            public void run(){
                
                try {
                    Thread.sleep(5000);
                    Socket processo1 = new Socket("localhost", 8001);
                    BufferedWriter escreveProcesso1 = new BufferedWriter(new OutputStreamWriter(processo1.getOutputStream()));
                    Socket processo3 = new Socket("localhost", 8003);
                    BufferedWriter escreveProcesso3 = new BufferedWriter(new OutputStreamWriter(processo3.getOutputStream()));

                    if (tipoMensagem.equals("MULTICAST")){
                        for(int i = 0; i < 1; i++) {
                            timestamp.incrementAndGet();
                            String id = "P2" + timestamp.toString();

                            escreveProcesso1.write("MULTICAST" + "?" + PID + "?" + timestamp.toString() + "?" + id);
                            escreveProcesso1.newLine();
                            escreveProcesso1.flush();

                            escreveProcesso3.write("MULTICAST" + "?" + PID + "?" + timestamp.toString() + "?" + id);
                            escreveProcesso3.newLine();
                            escreveProcesso3.flush();

                            String pk = Integer.toString(timestamp.intValue()) + PID;
                            filaMensagem.put(pk, id); //acrescentar chave valor na fila
                            filaAck.put(pk, 0); //acrescentar chave valor na fila
                        }

                    }else{
                        escreveProcesso1.write("ack" + "?" + PID + "?" + timestamp.toString() + "?" + tipoMensagem);
                        escreveProcesso1.newLine();
                        escreveProcesso1.flush();

                        escreveProcesso3.write("ack" + "?" + PID + "?" + timestamp.toString() + "?" + tipoMensagem);
                        escreveProcesso3.newLine();
                        escreveProcesso3.flush();
                    }
                    processo1.close();
                    processo3.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        lock.unlock();
    }

    public void run() {
        try{
            BufferedReader entradaProcesso2 = new BufferedReader(new InputStreamReader(p2Cliente.getInputStream()));
            String entrada = entradaProcesso2.readLine();

            do{
                StringTokenizer token = new StringTokenizer(entrada, "?");

                String tipoMensagem = token.nextToken();
                String sender = token.nextToken();
                String tempoRecebido = token.nextToken();
                String id = token.nextToken();

                System.out.println("Mensagem: " + tipoMensagem + ". id: " + id + ". origem: processo" + sender + ". timestamp: " + tempoRecebido);

                int tempoAtual = timestamp.intValue();
                int tempo = Integer.parseInt(tempoRecebido);
                
                if(tipoMensagem.equals("MULTICAST")){
                    filaMensagem.put(tempoRecebido + sender, id);
                    filaAck.put(tempoRecebido + sender,1);
                    
                    if(tempoAtual > tempo){
                        tempoAtual++;
                        timestamp.set(tempoAtual);
                    }else{
                        tempo++;
                        timestamp.set(tempo);
                    }
                    iniciaCliente(id);
                }else{
                    String chave = null;
                    for(Map.Entry<String, String> valor : filaMensagem.entrySet()){
                        if(valor.getValue().equals(id)){
                            chave = valor.getKey();
                            break;
                        }
                    }

                    Integer nAcks = filaAck.get(chave);
                    if(nAcks != null){
                        filaAck.replace(chave, nAcks, ++nAcks);

                        if( nAcks == 2 && (filaMensagem.firstKey().equals(chave))){
                            String m = filaMensagem.get(chave);
                            System.out.println("\tMensagem " + m + " entregue para todos os processos.");
                            filaMensagem.pollFirstEntry();
                            filaAck.pollFirstEntry();
                        }
                    }
                }
                entrada = entradaProcesso2.readLine();
            }while(entrada != null);
        }catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public static void main (String[] args) throws IOException {
        criaServer();
        iniciaServer();
        iniciaCliente("MULTICAST");
    }
}
