package fr.cnes.regards.microservices.backend.controlers;


public class SendTime implements Runnable {

    private Controler listener;

    public SendTime(Controler listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep( 2000 );
                listener.sendTime();
            } catch ( InterruptedException e ) {
                e.printStackTrace();
            }
        }   
    }
}