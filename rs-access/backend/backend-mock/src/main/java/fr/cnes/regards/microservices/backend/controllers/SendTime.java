package fr.cnes.regards.microservices.backend.controllers;


public class SendTime implements Runnable {

    private Controller listener;

    public SendTime(Controller listener) {
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