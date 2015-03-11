using System;
using System.Collections.Generic;
using System.Text;

namespace Flowy.Scheduler.CSharpSDK.Entities
{
    public class WorkerSetting
    {
        public string WorkerId { get; set; }

        public string WorkerName { get; set; }

        public string ExecuteTime { get; set; }

        public int Timeout { get; set; }
    }
}
