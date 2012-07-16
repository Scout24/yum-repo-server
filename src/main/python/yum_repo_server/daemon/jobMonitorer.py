import datetime

class JobMonitorer:
    t1=None
    t2=None
    
    
    def job_starts(self):
        self.t1=datetime.datetime.now()

    def job_finishes(self):
        self.t2=datetime.datetime.now()
        
    def get_execution_time_until_now_seconds(self):
        if self.t1 is None:
            return 0
        dt = datetime.datetime.now()-self.t1
        return dt.seconds 

    def get_execution_time_seconds(self):
        if self.t1 is None or self.t2 is None:
            return 0
        dt=self.t2-self.t1
        return dt.seconds

    def get_pretty_job_summary(self,job_description):
        if self.t1 is None:
            return "job %s has not started yet!"%job_description
        if self.t2 is None:
            return "job %s is not finished yet but has been working for %s seconds so far"%(job_description,str(self.get_execution_time_until_now_seconds()))
        return "job %s took %s seconds"%(job_description,str(self.get_execution_time_seconds()))