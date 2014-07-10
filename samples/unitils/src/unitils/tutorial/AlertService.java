package unitils.tutorial;

import java.util.*;

public final class AlertService
{
   private final SchedulerService schedulerService;
   private final MessageService messageService;

   public AlertService(SchedulerService schedulerService) { this(schedulerService, new MessageService()); }

   public AlertService(SchedulerService schedulerService, MessageService messageService)
   {
      this.schedulerService = schedulerService;
      this.messageService = messageService;
   }

   public void sendScheduledAlerts()
   {
      List<Message> scheduledAlerts = schedulerService.getScheduledAlerts("123", 1, true);

      for (Message alert : scheduledAlerts) {
         messageService.sendMessage(alert);
      }
   }
}
