import httplib

class IntegrationTestHelper():
    def __init__(self, host, port):
        self.host = host
        self.port = port
    
    def do_http_get(self, extPath):
        try:
            httpServ = httplib.HTTPConnection(self.host, self.port)
            httpServ.request('GET', extPath)
            response = httpServ.getresponse()
            return response
        except httplib.HTTPException:
            print "ERROR! Looks like the server is not running on " + self.host
            exit
