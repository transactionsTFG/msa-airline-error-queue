package domainevent.consumer;

import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import com.google.gson.Gson;

import domainevent.publisher.IJMSCommandPublisher;
import domainevent.services.MonitoringServices;
import msa.commons.consts.JMSQueueNames;
import msa.commons.consts.PropertiesConsumer;
import msa.commons.event.Event;
import msa.commons.event.EventId;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@MessageDriven(mappedName = JMSQueueNames.AIRLINE_MONITORING_ERROR_QUEUE)
public class CommandConsumerMonitoring implements MessageListener {
    private Gson gson;
    private MonitoringServices monitoringServices;
    private IJMSCommandPublisher jmsCommandPublisher;
    private static final Logger LOGGER = LogManager.getLogger(CommandConsumerMonitoring.class);

    @Override
    public void onMessage(Message msg) {
         try {
            if(msg instanceof TextMessage m) {
                String origin = m.getStringProperty(PropertiesConsumer.ORIGIN_QUEUE);
                Event event = this.gson.fromJson(m.getText(), Event.class);
                LOGGER.warn("Monitoreando en Cola {}, Evento Id: {}, Mensaje: {}", JMSQueueNames.AIRLINE_MONITORING_ERROR_QUEUE, event.getEventId(), event.getValue());
                this.monitoringServices.saveError(origin, event.getEventId(), event.getValue());
                for(EventId rollback : event.getValue().getRollbackEventTo()) 
                    this.jmsCommandPublisher.publish(rollback, event.getValue());
            }
        } catch (Exception e) {
            LOGGER.error("Error al recibir el mensaje: {}", e.getMessage());
        }
    }

    @Inject
    public void setGson(Gson gson) { this.gson = gson; }
    @Inject
    public void setMonitoringServices(MonitoringServices monitoringServices) { this.monitoringServices = monitoringServices; }
}