package com.lilith.leveldb.version;

import java.util.ArrayList;
import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;

import com.lilith.leveldb.api.Slice;
import com.lilith.leveldb.exceptions.BadFormatException;
import com.lilith.leveldb.util.BinaryUtil;
import com.lilith.leveldb.util.Settings;

public class VersionEdit {
  private Slice comparator;
  ArrayList<SimpleEntry<Integer, Long>> deleted_files = null;
  ArrayList<SimpleEntry<Integer, InternalKey>> compact_pointers = null;
  ArrayList<SimpleEntry<Integer, FileMetaData>> new_files = null;
  
  long log_num;
  long next_file_num;
  long last_seq;
  
  boolean has_log_num = false;
  boolean has_comparator = false;
  boolean has_next_file_num = false;
  boolean has_last_seq = false;

  // Tag numbers for serialized VersionEdit. These numbers are written to disk
  // and should not be changed.
  private final static int Comparator     = 1;
  private final static int LogNum         = 2;
  private final static int NextFileNum    = 3;
  private final static int LastSeq        = 4;
  private final static int CompactPointer = 5;
  private final static int DeletedFile    = 6;
  private final static int NewFile        = 7;
  
  public VersionEdit() {
    this.deleted_files = new ArrayList<SimpleEntry<Integer, Long>>();
    this.compact_pointers = new ArrayList<SimpleEntry<Integer, InternalKey>>();
    this.new_files = new ArrayList<SimpleEntry<Integer, FileMetaData>>();
    
    this.has_log_num = false;
    this.has_comparator = false;
    this.has_next_file_num = false;
    this.has_last_seq = false;
    
    this.log_num = 0;
    this.next_file_num = 0;
    this.last_seq = 0;
    this.comparator = Slice.EmptySlice;
  }
  
  public void Clear() {
    this.log_num = 0;
    this.last_seq = 0;
    this.next_file_num = 0;
    
    this.has_comparator = false;
    this.has_last_seq = false;
    this.has_next_file_num = false;
    this.has_log_num = false;
    
    comparator = Slice.EmptySlice;
    
    deleted_files.clear();
    new_files.clear();
  }
  
  public void SetComparatorName(Slice name) {
    this.comparator = name;
    this.has_comparator = true;
  }
  
  public void SetLogNumber(long num) {
    this.has_log_num = true;
    this.log_num = num;
  }
  
  public void SetNextFile(long num) {
    this.next_file_num = num;
    this.has_next_file_num = true;
  }
  
  public void SetLastSequence(long seq) {
    this.has_last_seq = true;
    this.last_seq = seq;
  }
  
  public void SetCopmactionPointer(int level, InternalKey key) {
    compact_pointers.add(new SimpleEntry<Integer, InternalKey>(level, key));
  }
  
  /**
   * Add the specified file at the specified number.
   */
  public void AddFile(int level, long file, int file_size, InternalKey smallest, InternalKey largest) {
    FileMetaData file_meta = new FileMetaData(file, file_size, smallest, largest);
    new_files.add(new SimpleEntry<Integer, FileMetaData>(level, file_meta));
  }
  
  /**
   * Delete the specified file from the specified level.
   */
  public void DeleteFile(int level, long file) {
    deleted_files.add(new SimpleEntry<Integer, Long>(level, file));
  }
  
  public Slice EncodeTo() {
    byte[] buffer = new byte[GetApproximateSize()];
    int offset = 0;
    if (has_comparator) {
      BinaryUtil.PutVarint32(buffer, offset, Comparator);
      BinaryUtil.PutVarint32(buffer, offset + Settings.UINT32_SIZE, comparator.GetLength());
      BinaryUtil.CopyBytes(comparator.GetData(), comparator.GetOffset(), comparator.GetLength(), buffer, offset + Settings.UINT32_SIZE * 2);
      offset += comparator.GetLength() + Settings.UINT32_SIZE * 2;
    }
    
    if (has_log_num) {
      BinaryUtil.PutVarint32(buffer, offset, LogNum);
      BinaryUtil.PutVarint64(buffer, offset + Settings.UINT32_SIZE, log_num);
      offset += Settings.UINT32_SIZE + Settings.UINT64_SIZE;
    }
    
    if (has_next_file_num) {
      BinaryUtil.PutVarint32(buffer, offset, NextFileNum);
      BinaryUtil.PutVarint64(buffer, offset + Settings.UINT32_SIZE, next_file_num);
      offset += Settings.UINT32_SIZE + Settings.UINT64_SIZE;
    }
    
    if (has_last_seq) {
      BinaryUtil.PutVarint32(buffer, offset, LastSeq);
      BinaryUtil.PutVarint64(buffer, offset + Settings.UINT32_SIZE, last_seq);
      offset += Settings.UINT32_SIZE + Settings.UINT64_SIZE;
    }
    
    for (int i = 0; i < compact_pointers.size(); i++) {
      BinaryUtil.PutVarint32(buffer, offset, CompactPointer); offset += Settings.UINT32_SIZE;
      BinaryUtil.PutVarint32(buffer, offset, compact_pointers.get(i).getKey()); offset += Settings.UINT32_SIZE;
      
      Slice internal_key = compact_pointers.get(i).getValue().Encode();
      BinaryUtil.PutVarint32(buffer, offset, internal_key.GetLength()); offset += Settings.UINT32_SIZE;
      BinaryUtil.CopyBytes(internal_key.GetData(), internal_key.GetOffset(), internal_key.GetLength(), buffer, offset);
    }
    
    Iterator<SimpleEntry<Integer, Long>> del_iter = deleted_files.iterator();
    while (del_iter.hasNext()) {
      SimpleEntry<Integer, Long> val = del_iter.next();
      BinaryUtil.PutVarint32(buffer, offset, DeletedFile);
      BinaryUtil.PutVarint32(buffer, offset + Settings.UINT32_SIZE, val.getKey());
      BinaryUtil.PutVarint64(buffer, offset + Settings.UINT32_SIZE * 2, val.getValue());
      offset += Settings.UINT32_SIZE * 2 + Settings.UINT64_SIZE;
    }
  
    Iterator<SimpleEntry<Integer, FileMetaData>> add_iter = new_files.iterator();
    while (add_iter.hasNext()) {
      SimpleEntry<Integer, FileMetaData> entry = add_iter.next();
      BinaryUtil.PutVarint32(buffer, offset, NewFile); offset += Settings.UINT32_SIZE;
      BinaryUtil.PutVarint32(buffer, offset, entry.getKey()); offset += Settings.UINT32_SIZE;
      BinaryUtil.PutVarint64(buffer, offset, entry.getValue().number); offset += Settings.UINT64_SIZE;
      BinaryUtil.PutVarint64(buffer, offset, entry.getValue().file_size); offset += Settings.UINT64_SIZE;
      
      Slice small = entry.getValue().smallest.Encode();
      BinaryUtil.PutVarint32(buffer, offset, small.GetLength()); offset += Settings.UINT32_SIZE;
      BinaryUtil.CopyBytes(small.GetData(), small.GetOffset(), small.GetLength(), buffer, offset);
      
      Slice large = entry.getValue().largest.Encode();
      BinaryUtil.PutVarint32(buffer, offset, large.GetLength()); offset += Settings.UINT32_SIZE;
      BinaryUtil.CopyBytes(large.GetData(), large.GetOffset(), large.GetLength(), buffer, offset);
    }
    return new Slice(buffer);
  }
  
  public int GetApproximateSize() {
    int size = 0;
    if (has_comparator) size += Settings.UINT32_SIZE + Settings.UINT32_SIZE + comparator.GetLength();
    if (has_log_num)    size += Settings.UINT32_SIZE + Settings.UINT64_SIZE;
    if (has_next_file_num) size += Settings.UINT32_SIZE + Settings.UINT64_SIZE;
    if (has_last_seq) size += Settings.UINT32_SIZE + Settings.UINT64_SIZE;
    
    for (int i = 0; i < compact_pointers.size(); i++) {
      size += Settings.UINT32_SIZE * 3;
      size += compact_pointers.get(i).getValue().GetInternalKeySize();
    }
    
    size += (Settings.UINT32_SIZE * 2 + Settings.UINT64_SIZE) * deleted_files.size();
    
    Iterator<SimpleEntry<Integer, FileMetaData>> add_iter = new_files.iterator();
    while (add_iter.hasNext()) {
      SimpleEntry<Integer, FileMetaData> entry = add_iter.next();
      size += Settings.UINT32_SIZE * 2 + Settings.UINT64_SIZE * 2;
      size += entry.getValue().largest.GetInternalKeySize() + Settings.UINT32_SIZE;
      size += entry.getValue().smallest.GetInternalKeySize() + Settings.UINT32_SIZE;
    }
    return size;
  }
  
  public void DecodeFrom(byte[] data, int offset, int size) throws BadFormatException {
    Clear();
    
    int end_of_input = offset + size;
    int cur_pos = offset;
    
    while (cur_pos < end_of_input) {
      int val_type = BinaryUtil.DecodeVarint32(data, cur_pos); cur_pos += Settings.UINT32_SIZE;
      int data_len = ParseContent(data, cur_pos, val_type);
      if (data_len == -1) throw new BadFormatException("version edit parse error");
      cur_pos += data_len;
    }
  }
  
  public void DecodeFrom(Slice input) throws BadFormatException  {
    DecodeFrom(input.GetData(), input.GetOffset(), input.GetLength());
  }
  
  private int ParseContent(byte[] data, int offset, int val_type) {
    switch (val_type) {
    case Comparator:
      Slice str = Slice.GetLengthPrefix(data, offset);
      this.comparator = str;
      this.has_comparator = true;
      return Settings.UINT32_SIZE + str.GetLength();
    case LogNum:
      this.log_num = BinaryUtil.DecodeVarint64(data, offset);
      this.has_log_num = true;
      return Settings.UINT64_SIZE;
    case NextFileNum:
      this.next_file_num = BinaryUtil.DecodeVarint64(data, offset);
      this.has_next_file_num = true;
      return Settings.UINT64_SIZE;
    case LastSeq:
      this.last_seq = BinaryUtil.DecodeVarint64(data, offset);
      this.has_last_seq = true;
      return Settings.UINT64_SIZE;
    case CompactPointer:
      int comp_level = GetLevel(data, offset);
      InternalKey comp_key = GetInternalKey(data, offset + Settings.UINT32_SIZE);
      this.compact_pointers.add(new SimpleEntry<Integer, InternalKey>(comp_level, comp_key));
      return Settings.UINT32_SIZE * 3 + comp_key.GetInternalKeySize();
    case DeletedFile:
      int del_level = GetLevel(data, offset);
      long del_file_num = BinaryUtil.DecodeVarint64(data, offset + Settings.UINT32_SIZE);
      this.deleted_files.add(new SimpleEntry<Integer, Long>(del_level, del_file_num));
      return Settings.UINT32_SIZE + Settings.UINT64_SIZE;
    case NewFile:
      int new_level = GetLevel(data, offset); offset += Settings.UINT32_SIZE;
      long new_file_num = BinaryUtil.DecodeVarint64(data, offset); offset += Settings.UINT64_SIZE;
      long new_file_size = BinaryUtil.DecodeVarint64(data, offset); offset += Settings.UINT64_SIZE;
      InternalKey small = GetInternalKey(data, offset); offset += Settings.UINT32_SIZE + small.GetInternalKeySize();
      InternalKey large = GetInternalKey(data, offset);
      FileMetaData file_meta = new FileMetaData(new_file_num, (int) new_file_size, small, large);
      this.new_files.add(new SimpleEntry<Integer, FileMetaData>(new_level, file_meta));
      return Settings.UINT64_SIZE * 2 + Settings.UINT32_SIZE * 3 + small.GetInternalKeySize() +
             large.GetInternalKeySize();
    default:
      return -1;
    }
  }
  
  private InternalKey GetInternalKey(byte[] data, int offset) {
    Slice rep = Slice.GetLengthPrefix(data, offset);
    InternalKey internal_key = new InternalKey();
    internal_key.DecodeFrom(rep.GetData(), rep.GetOffset(), rep.GetLength());
    return internal_key;
  }
  
  private int GetLevel(byte[] data, int offset) {
    return BinaryUtil.DecodeVarint32(data, offset);
  }
}
