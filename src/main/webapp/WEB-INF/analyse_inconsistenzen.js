/**
 * Created with IntelliJ IDEA.
 * User: lgohlke
 * Date: 07.05.13
 * Time: 09:40
 * To change this template use File | Settings | File Templates.
 */


function removeStaleFsEntries(entry) {
    var checksum = entry.metadata.sha256;
    if ('undefined' != typeof(checksum)) {
        var result = db.yum.entries.findOne({'yumPackage.checksum.checksum': checksum});
        if (null == result) {
            print("found missing entry in fs.files for '" + entry._id + "'");
//            print(" " + tojson(entry.filename))
//            print( " " + tojson(entry))
            if (removeStaleEntries) {
                staleFsFilesCounter++
                db.fs.files.remove(entry);
            }
        }
    }

    countFs++;

    if (countFs % 1000 == 0) {
        print(".")
    }
}

rs.slaveOk()

var countYum = 0;

var countFs = 0;
var removeStaleEntries = false
var staleFsFilesCounter = 0
db.fs.files.find({"filename": /rpm/, "uploadDate": {"$lt": new Date(Date.now() - 24 * 60 * 60 * 1000)}}).forEach(removeStaleFsEntries)

print("staled fs entries " + staleFsFilesCounter)
print("removed staled fs entries " + removeStaleEntries)

print("checked fs " + countFs)
print("checked yum " + countYum)
