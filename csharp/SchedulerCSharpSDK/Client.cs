using System;
using System.Collections.Generic;
using System.Text;
using System.Net.Sockets;
using Flowy.Scheduler.CSharpSDK.Entities;
using Flowy.Scheduler.CSharpSDK.Protocal;
using System.IO;

namespace Flowy.Scheduler.CSharpSDK
{
    public class Client
    {
        public const int DEFAULT_HOST_PORT = 3333;

        private string[] m_hosts;

        private string m_app_key;

        private string m_app_secret;

        private TcpClient m_socket;

        private WorkerSetting m_setting;

        private Task m_current_task;

        public delegate void OnTaskNotifyEventHandler(object sender, TaskNotifyEventArgs e);

        public event OnTaskNotifyEventHandler OnNotify;

        /// <summary>
        /// 实例化Client
        /// </summary>
        /// <param name="hosts">host:port 多个地址用逗号分隔</param>
        /// <param name="workerSetting">需要创建的worker设置</param>
        public Client(String hosts, string app_key, string app_secret, WorkerSetting workerSetting)
        {
            m_hosts = hosts.Split(';');
            m_app_key = app_key;
            m_app_secret = app_secret;
            m_setting = workerSetting;
        }

        public void Start()
        {
            string host = m_hosts[0];
            string host_name;   // 远程服务器地址
            int host_port;      // 远程服务器端口

            string[] parts = host.Split(':');
            host_name = parts[0];
            host_port = DEFAULT_HOST_PORT;
            if (parts.Length > 0)
            {
                host_port = int.Parse(parts[1]);
            }
            m_socket = new TcpClient(host_name, host_port);

            SendVersion();

            Auth();

            Register();

            while (true)
            {
                StartListen();
            }
        }

        public void Stop()
        {
            m_socket.Close();
        }

        public void TaskStart()
        {
            TaskStatusUpdate update = new TaskStatusUpdate.Builder()
            {
                TaskId = m_current_task.Id,
                WorkerId = m_current_task.WorkerId,
                Status = TaskStatusUpdate.Types.Status.START
            }.Build();

            using (MemoryStream stream = new MemoryStream())
            {
                update.WriteTo(stream);
                SendMessage(stream.ToArray());
            }
        }

        public void TaskRunning(int percentage)
        {
            TaskStatusUpdate update = new TaskStatusUpdate.Builder() { 
                TaskId = m_current_task.Id, 
                WorkerId = m_current_task.WorkerId, 
                Status = TaskStatusUpdate.Types.Status.RUNNING }.Build();

            using (MemoryStream stream = new MemoryStream())
            {
                update.WriteTo(stream);
                SendMessage(stream.ToArray());
            }
        }

        public void TaskComplete()
        {
            TaskStatusUpdate update = new TaskStatusUpdate.Builder()
            {
                TaskId = m_current_task.Id,
                WorkerId = m_current_task.WorkerId,
                Status = TaskStatusUpdate.Types.Status.STOP
            }.Build();

            using (MemoryStream stream = new MemoryStream())
            {
                update.WriteTo(stream);
                SendMessage(stream.ToArray());
            }
        }

        public void TaskFailed()
        {
            TaskStatusUpdate update = new TaskStatusUpdate.Builder()
            {
                TaskId = m_current_task.Id,
                WorkerId = m_current_task.WorkerId,
                Status = TaskStatusUpdate.Types.Status.STOP
            }.Build();

            using (MemoryStream stream = new MemoryStream())
            {
                update.WriteTo(stream);
                SendMessage(stream.ToArray());
            }
        }



        private void SendVersion()
        {
            string version = "FSP_0.0.1\0";
            byte[] sendBuffer = Encoding.ASCII.GetBytes(version);
            m_socket.GetStream().Write(sendBuffer, 0, sendBuffer.Length);
        }


        private void Auth()
        {
            LoginRequest loginRequest = new LoginRequest.Builder() { AppId = m_app_key, AppSecret = m_app_secret }.Build();
            using (MemoryStream stream = new MemoryStream())
            {
                loginRequest.WriteTo(stream);
                byte[] loginRequestBuffer = stream.ToArray();
                SendMessage(loginRequestBuffer);
            }
            LoginResponse loginResponse = LoginResponse.ParseFrom(ReadNextMessage());
            System.Console.WriteLine(loginResponse);

        }

        private void Register()
        {
            WorkerRegisterRequest request = new WorkerRegisterRequest.Builder()
            {
                WorkerId = m_setting.WorkerId,
                WorkerName = m_setting.WorkerName,
                Timeout = m_setting.Timeout,
                ExecuteLastExpired = WorkerRegisterRequest.Types.ExecuteLastExpiredType.IGNORE
            }.AddExecuteTime(m_setting.ExecuteTime).Build();
            

            using (MemoryStream stream = new MemoryStream())
            {
                request.WriteTo(stream);
                byte[] buffer = stream.ToArray();
                SendMessage(buffer);
            }

            WorkerRegisterResponse response = WorkerRegisterResponse.ParseFrom(ReadNextMessage());

        }

        private void StartListen()
        {
            TaskNotify notify = TaskNotify.ParseFrom(ReadNextMessage());
            Task task = new Task { Id = notify.TaskId, WorkerId = notify.WorkerId };
            m_current_task = task;
            if (OnNotify != null)
            {
                OnNotify(this, new TaskNotifyEventArgs(this, task));
            }
        }

        private byte[] ReadNextMessage()
        {
            byte[] sizeBuffer = new byte[4];
            m_socket.GetStream().Read(sizeBuffer, 0, 4);

            List<byte> sizeBufferList = new List<byte>(sizeBuffer);
            if (BitConverter.IsLittleEndian)
            {
                sizeBufferList.Reverse();
            }

            int messageSize = BitConverter.ToInt32(sizeBufferList.ToArray(), 0);

            byte[] messageBuffer = new byte[messageSize];
            m_socket.GetStream().Read(messageBuffer, 0, messageSize);

            return messageBuffer;
        }

        private void SendMessage(byte[] message)
        {
            int size = message.Length;
            List<byte> sizeBufferList = new List<byte>(BitConverter.GetBytes(size));
            if (BitConverter.IsLittleEndian)
            {
                sizeBufferList.Reverse();
            }

            m_socket.GetStream().Write(sizeBufferList.ToArray(), 0, 4);

            m_socket.GetStream().Write(message, 0, size);
        }

    }
}
