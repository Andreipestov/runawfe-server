package ru.runa.wfe.service.impl;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.Interceptors;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ejb.interceptor.SpringBeanAutowiringInterceptor;
import ru.runa.wfe.commons.Utils;
import ru.runa.wfe.execution.Token;
import ru.runa.wfe.execution.dao.TokenDao;
import ru.runa.wfe.service.interceptors.EjbExceptionSupport;
import ru.runa.wfe.service.interceptors.PerformanceObserver;

@MessageDriven(activationConfig = { @ActivationConfigProperty(propertyName = "destination", propertyValue = NodeAsyncFailedExecutionBean.QUEUE),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue") })
@TransactionManagement(TransactionManagementType.CONTAINER)
@Interceptors({ EjbExceptionSupport.class, PerformanceObserver.class, SpringBeanAutowiringInterceptor.class })
public class NodeAsyncFailedExecutionBean implements MessageListener {
    public static final String QUEUE = "queue/nodeAsyncFailedExecution";
    private static final Log log = LogFactory.getLog(NodeAsyncFailedExecutionBean.class);
    @Resource
    private MessageDrivenContext context;
    @Autowired
    private TokenDao tokenDao;

    @Override
    public void onMessage(Message jmsMessage) {
        try {
            ObjectMessage message = (ObjectMessage) jmsMessage;
            log.debug("On message " + message.getJMSMessageID());
            Long tokenId = message.getLongProperty("tokenId");
            String errorMessage = message.getStringProperty("errorMessage");
            if (errorMessage == null) {
                errorMessage = "DLQ";
            }
            Token token = tokenDao.get(tokenId);
            if (token == null) {
                log.info("Seems like process for tokenId = " + tokenId + " is removed");
                return;
            }
            if (token.getProcess().hasEnded()) {
                log.info("Ignored message for ended process, tokenId = " + tokenId);
                return;
            }
            Utils.failProcessExecution(token, errorMessage);
        } catch (Exception e) {
            log.error(jmsMessage, e);
            context.setRollbackOnly();
        }
    }

}