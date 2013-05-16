PRAGMA synchronous="OFF";
PRAGMA locking_mode="EXCLUSIVE";
CREATE TABLE db_info (dbversion INTEGER, checksum TEXT);
CREATE TABLE filelist (  pkgKey INTEGER,  dirname TEXT,  filenames TEXT,  filetypes TEXT);
CREATE TABLE packages (  pkgKey INTEGER PRIMARY KEY,  pkgId TEXT);
CREATE INDEX dirnames ON filelist (dirname);
CREATE INDEX keyfile ON filelist (pkgKey);
CREATE INDEX pkgId ON packages (pkgId);
CREATE TRIGGER remove_filelist AFTER DELETE ON packages BEGIN DELETE FROM filelist WHERE pkgKey = old.pkgKey; END;