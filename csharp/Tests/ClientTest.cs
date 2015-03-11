using System;
using System.Collections.Generic;
using System.Text;
using NUnit.Framework;
using Flowy.Scheduler.CSharpSDK.Entities;
using Flowy.Scheduler.CSharpSDK;
using System.Threading;

namespace Tests
{
    [TestFixture]
    public class ClientTest
    {


        [Test]
        public void ConnectHost()
        {
            string hosts = "localhost:8080";
            string app_key = "123";
            string app_secret = "321";

            WorkerSetting setting = new WorkerSetting
            {
                WorkerId = "CSharpClientTest",
                WorkerName = "CSharpClientTest",
                ExecuteTime = "*/5 * * * * ?",
                Timeout = 30
            };

            Client target = new Client(hosts, app_key, app_secret, setting);
            target.OnNotify += new Client.OnTaskNotifyEventHandler(target_OnNotify);

            target.Start();

        }

        void target_OnNotify(object sender, TaskNotifyEventArgs e)
        {
            System.Console.WriteLine(e.Task);
            e.Client.TaskStart();

            Thread.Sleep(2000);

            e.Client.TaskRunning(0);

            Thread.Sleep(2000);
            e.Client.TaskRunning(0);

            Thread.Sleep(2000);
            e.Client.TaskComplete();

            e.Client.Stop();
        }
    }
}
