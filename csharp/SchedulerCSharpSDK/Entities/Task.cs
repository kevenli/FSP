using System;
using System.Collections.Generic;
using System.Text;

namespace Flowy.Scheduler.CSharpSDK.Entities
{
    /// <summary>
    /// 任务实例对象
    /// </summary>
    public class Task
    {
        /// <summary>
        /// 客户端Task ID
        /// </summary>
        public string Id { get; set; }

        /// <summary>
        /// 客户端Worker ID
        /// </summary>
        public string WorkerId {get;set;}
    }
}
