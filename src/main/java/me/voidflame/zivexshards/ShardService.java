package me.voidflame.zivexshards;

import java.util.UUID;

public interface ShardService {
    long getBalance(UUID player);
    boolean deposit(UUID player, long amount);
    boolean withdraw(UUID player, long amount);
    boolean setBalance(UUID player, long amount);
    boolean transfer(UUID from, UUID to, long amount);
}
