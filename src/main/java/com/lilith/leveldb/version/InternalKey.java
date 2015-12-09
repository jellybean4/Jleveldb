package com.lilith.leveldb.version;

import java.util.Comparator;

import com.lilith.leveldb.api.Slice;
import com.lilith.leveldb.util.BinaryUtil;
import com.lilith.leveldb.util.Settings;

public class InternalKey implements Comparable<InternalKey> {
  private long seq = 0;
  private byte op_type = 0;
  private Slice key = null;

  private byte[] rep = null;
  private boolean updated = true;
  
  public InternalKey() {
    this.seq = 0;
    this.op_type = 0;
    this.key = null;
    this.rep = null;
    this.updated = true;
  }
  
  public InternalKey(Slice key, long seq, byte op_type) {
    this.seq = seq;
    this.key = key;
    this.op_type = op_type;
    this.updated = true;
  }
  
  
  public void DecodeFrom(byte[] rep) {
    this.rep = rep;
    this.key = Slice.DecodeLengthPrefix(rep, 0);
    long seq_type = BinaryUtil.DecodeVarint64(rep, key.GetLength() + Settings.UINT32_SIZE);
    this.op_type = (byte) (seq_type & 0XFF);
    this.seq = seq_type >> 8;        
  }
  
  public Slice Encode() {
    if (updated == false) return new Slice(rep);
    rep = new byte[Settings.UINT64_SIZE + key.GetLength()];
    BinaryUtil.CopyBytes(key.GetData(), key.GetOffset(), key.GetLength(), rep, 0);
    BinaryUtil.PutVarint64(rep, key.GetLength(), (seq << 8) & op_type);
    this.updated = false;
    return new Slice(rep);
  }
  
  public int GetInternalKeySize() {
    return Encode().GetLength();
  }


  public int compareTo(InternalKey o) {
    // TODO Auto-generated method stub
    return 0;
  }
}
