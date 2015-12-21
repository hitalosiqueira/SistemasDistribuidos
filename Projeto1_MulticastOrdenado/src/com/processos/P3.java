import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/*Criado por Hitalo Siqueira */

public class P3 extends Thread {

    private static TreeMap<String, String> filaMensagem = new TreeMap<String, String>();
    private static TreeMap<String, Integer> filaAck = new TreeMap<String, Integer>();
    private static final String PID = "3";
    private static AtomicInteger timestamp = new AtomicInteger(0);
    private static ServerSocket server;
    private Socket p3Cliente;
    private static ReentrantLock lock = new ReentrantLock();

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

    public static void iniciaCliente( String tipoMensagem ){
        lock.lock();
        new Thread(){
            public void run(){
                
                try {
                    Thread.sleep(5000);
                    Socket processo2 = new Socket("localhost", 8002);
                    BufferedWriter escreveProcesso2 = new BufferedWriter(new OutputStreamWriter(processo2.getOutputStream()));
                    Socket processo1 = new Socket("localhost", 8001);
                    BufferedWriter escreveProcesso1 = new BufferedWriter(new OutputStreamWriter(processo1.getOutputStream()));

                    if (tipoMensagem.equals("MULTICAST")){
                        for(int i = 0; i < 1; i++) {
                            timestamp.incrementAndGet();
                            String id = "P3" + timestamp.toString();

                            escreveProcesso2.write("MULTICAST" + "?" + PID + "?" + timestamp.toString() + "?" + id);
                            escreveProcesso2.newLine();
                            escreveProcesso2.flush();

                            escreveProcesso1.write("MULTICAST" + "?" + PID + "?" + timestamp.toString() + "?" + id);
                            escreveProcesso1.newLine();
                            escreveProcesso1.flush();

                            String pk = Integer.toString(timestamp.intValue()) + PID;
                            filaMensagem.put(pk, id); //acrescentar chave valor na fila
                            filaAck.put(pk, 0); //acrescentar chave valor na fila
                        }

                    }else{

                        escreveProcesso2.write("ack" + "?" + PID + "?" + timestamp.toString() + "?" + tipoMensagem);
                        escreveProcesso2.newLine();
                        escreveProcesso2.flush();

                        escreveProcesso1.write("ack" + "?" + PID + "?" + timestamp.toString() + "?" + tipoMensagem);
                        escreveProcesso1.newLine();
                        escreveProcesso1.flush();
                    }
                    processo2.close();
                    processo1.close();
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
            BufferedReader entradaProcesso3 = new BufferedReader(new InputStreamReader(p3Cliente.getInputStream()));
            String entrada = entradaProcesso3.readLine();

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
                    filaAck.put(tempoRecebido + sender, 1);
                    
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
                entrada = entradaProcesso3.readLine();
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
