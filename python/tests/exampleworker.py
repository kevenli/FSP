
from fsp.client import AsyncClient, Task
import logging
import time
import threading

logger = logging.getLogger(__name__)

def job_start():
    logger.info("start")
    time.sleep(10)
    logger.info('Still running')
    time.sleep(10)
    logger.info('Still running')
    time.sleep(10)
    logger.info('Finished')



def main():
    client = AsyncClient('localhost', 'abc', '123')
    def register_job():
        job = Task('Ajob', '*/5 * * * * ?')
        client.register_task(job, job_start)

    #client.connect(callback=register_job)
    client.connect()
    client.start()

if __name__ == '__main__':
    main()