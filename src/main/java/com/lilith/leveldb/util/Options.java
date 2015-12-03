package com.lilith.leveldb.util;

public class Options {
  // if true, the database will be created if it is missing.
  public boolean creat_if_missing = true;
  
  // if true, an error is raised if the database already exists.
  public boolean error_if_existing = false;
  
  // if true, the implementation will do aggressive checking of the
  // data it is processing and will stop early if it detects any errors.
  // This may have unforeseen ramifications: for example, a corruption
  // of one DB entry may cause a large number of entries to become unreadable
  // or for the entire DB to become unopenable.
  public boolean paranoid_checks = false;
  
  
  // Any internal progress/error information generated by the db will be
  // written to log files in logger_dir if it is not null. Or to a file stored
  // in the same directory as the DB contents if logger_dir is null.
  public String logger_dir = null;
  
  // Amount of data to build up in memory(backed by an unsorted log on disk) before
  // converting into a sorted-on-disk file.
  //
  // Larger values increase performance, especially during bulk loads. Up to two write
  // buffers may be held in memory at the same time, so you may wish to adjust this
  // parameter to control memory usage. Also, a larger write buffer will result in a
  // longer recovery time the next time the database is opened.
  public int write_buffer_size = 1024 * 1024 * 4;
  
  // Number of open files that can be opened by the db. you may need to increase this if
  // your database has a larger working set (budget one open file per 2MB of working set).
  public int max_open_files = 1000;
  
  // Approximate size of user data packed per block. Note that the block size specified here
  // corresponds to uncompressed data. The actual size of the unit read from disk may be smaller
  // if compression is enabled. This parameter can be changed dynamically.
  public int block_size = 1024 * 4;
  
  // Number of keys between restart points for delta encoding of keys. This parameter can be
  // changed dynamically. Most clients should leave this parameter alone.
  public int block_restart_interval = 16;
  
  // Compress blocks using the specified compression algorithm. This parameter can be changed dynamically.
  // Default: SnappyCompression, which gives lightweight but fast compression.
  public int compression_type = Settings.SnappyCompression;
  
  // use the specified filter policy to reduce disk reads. Many applications will benefit from setting
  // BLOOM_FILTER_POLICY here
  public int filter_policy = Settings.BLOOM_FILTER_POLICY;
}





