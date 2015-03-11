using System;
using System.Collections.Generic;
using System.Text;
using Flowy.Scheduler.CSharpSDK.Entities;

namespace Flowy.Scheduler.CSharpSDK
{
    public class TaskNotifyEventArgs : EventArgs
    {
        private Task m_task;
        private Client m_client;
        public TaskNotifyEventArgs(Client client, Task task)
        {
            m_client = client;
            m_task = task;
        }

        public Task Task {
            get { return m_task; }
        }

        public Client Client
        {
            get
            {
                return m_client;
            }
        }
    }
}
