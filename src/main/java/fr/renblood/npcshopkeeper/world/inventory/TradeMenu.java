package fr.renblood.npcshopkeeper.world.inventory;


import com.mojang.brigadier.context.CommandContext;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.network.TradeSlotMessage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import fr.renblood.npcshopkeeper.init.NpcshopkeeperModMenus;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;



public class TradeMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
	public final static HashMap<String, Object> guistate = new HashMap<>();
	public final Level world;
	public final Player entity;
	public int x, y, z;
	private ContainerLevelAccess access = ContainerLevelAccess.NULL;
	private IItemHandler internal;
	private final Map<Integer, Slot> customSlots = new HashMap<>();
    private boolean settlementPending;
    public boolean isSettlementPending() { return settlementPending; }
    public void beginSettlement() { settlementPending = true; }
    public void endSettlement() { settlementPending = false; }
	private boolean bound = false;
	private Supplier<Boolean> boundItemMatcher = null;
	private Entity boundEntity = null;
	private BlockEntity boundBlockEntity = null;
    
    // Stockage des infos du trade
    public String tradeName = "";
    public String npcId = "";
    public String npcName = "";

    private static boolean isRealSlot(int slot){return slot==1||slot==3||slot==5||slot==7||slot==8||slot==9||slot==10||slot==11;}
    private void persistEscrow(){
        if(world==null||world.isClientSide||internal==null)return;
        net.minecraft.nbt.ListTag escrow=new net.minecraft.nbt.ListTag();
        for(int slot=0;slot<internal.getSlots();slot++)if(isRealSlot(slot)&&!internal.getStackInSlot(slot).isEmpty()){
            net.minecraft.nbt.CompoundTag row=internal.getStackInSlot(slot).save(new net.minecraft.nbt.CompoundTag());row.putInt("Slot",slot);escrow.add(row);
        }
        entity.getPersistentData().put("RenbloodTradeEscrow",escrow);
    }

	public TradeMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
		super(NpcshopkeeperModMenus.TRADE.get(), id);
		this.entity = inv.player;
		this.world = inv.player.level();
		this.internal = new ItemStackHandler(16){
            @Override protected void onContentsChanged(int slot){persistEscrow();}
        };
        if(!world.isClientSide){
            net.minecraft.nbt.ListTag escrow=entity.getPersistentData().getList("RenbloodTradeEscrow",10).copy();
            for(int i=0;i<escrow.size();i++){
                net.minecraft.nbt.CompoundTag row=escrow.getCompound(i);int slot=row.getInt("Slot");
                if(isRealSlot(slot))((ItemStackHandler)internal).setStackInSlot(slot,ItemStack.of(row));
            }
            persistEscrow();
        } // Augmenté à 16 pour inclure le slot XP (index 15)
		
		BlockPos pos = null;
		if (extraData != null) {
			pos = extraData.readBlockPos();
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
			access = ContainerLevelAccess.create(world, pos);
			
			// Lecture des données supplémentaires envoyées par TradeCommandProcedure
			// Ordre : BlockPos, String (tradeName), String (npcId), String (npcName)
			if (extraData.readableBytes() > 0) {
			    try {
                    this.tradeName = extraData.readUtf();
                    this.npcId = extraData.readUtf();
                    this.npcName = extraData.readUtf();
                } catch (Exception e) {
                    // Si la lecture échoue (ex: ancien format ou autre appel), on ignore silencieusement
                    // pour ne pas crash, mais on loggue si possible.
                }
			}
		}
		
		// La logique "bound to entity/block" générée par MCreator est supprimée ici car elle entrait en conflit
		// avec nos données personnalisées. On suppose que le trade est toujours "virtuel" ou lié au BlockPos.

		this.customSlots.put(0, this.addSlot(new SlotItemHandler(internal, 0, 14, 19) {
			private final int slot = 0;

			@Override
			public boolean mayPickup(Player entity) {
				return false;
			}

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		this.customSlots.put(1, this.addSlot(new SlotItemHandler(internal, 1, 44, 19) {
			private final int slot = 1;
		}));
		this.customSlots.put(2, this.addSlot(new SlotItemHandler(internal, 2, 14, 49) {
			private final int slot = 2;

			@Override
			public boolean mayPickup(Player entity) {
				return false;
			}

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		this.customSlots.put(3, this.addSlot(new SlotItemHandler(internal, 3, 44, 49) {
			private final int slot = 3;
		}));
		this.customSlots.put(4, this.addSlot(new SlotItemHandler(internal, 4, 80, 19) {
			private final int slot = 4;

			@Override
			public boolean mayPickup(Player entity) {
				return false;
			}

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		this.customSlots.put(5, this.addSlot(new SlotItemHandler(internal, 5, 110, 19) {
			private final int slot = 5;
		}));
		this.customSlots.put(6, this.addSlot(new SlotItemHandler(internal, 6, 80, 49) {
			private final int slot = 6;

			@Override
			public boolean mayPickup(Player entity) {
				return false;
			}

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		this.customSlots.put(7, this.addSlot(new SlotItemHandler(internal, 7, 110, 49) {
			private final int slot = 7;
		}));
		this.customSlots.put(8, this.addSlot(new SlotItemHandler(internal, 8, 146, 9) {
			private final int slot = 8;

			@Override
			public void onTake(Player entity, ItemStack stack) {
				super.onTake(entity, stack);
				slotChanged(8, 1, 0);
			}

			@Override
			public void onQuickCraft(ItemStack a, ItemStack b) {
				super.onQuickCraft(a, b);
				slotChanged(8, 2, b.getCount() - a.getCount());
			}

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		this.customSlots.put(9, this.addSlot(new SlotItemHandler(internal, 9, 170, 9) {
			private final int slot = 9;

			@Override
			public void onTake(Player entity, ItemStack stack) {
				super.onTake(entity, stack);
				slotChanged(9, 1, 0);
			}

			@Override
			public void onQuickCraft(ItemStack a, ItemStack b) {
				super.onQuickCraft(a, b);
				slotChanged(9, 2, b.getCount() - a.getCount());
			}

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		this.customSlots.put(10, this.addSlot(new SlotItemHandler(internal, 10, 158, 33) {
			private final int slot = 10;

			@Override
			public void onTake(Player entity, ItemStack stack) {
				super.onTake(entity, stack);
				slotChanged(10, 1, 0);
			}

			@Override
			public void onQuickCraft(ItemStack a, ItemStack b) {
				super.onQuickCraft(a, b);
				slotChanged(10, 2, b.getCount() - a.getCount());
			}

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		this.customSlots.put(11, this.addSlot(new SlotItemHandler(internal, 11, 158, 58) {
			private final int slot = 11;

			@Override
			public void onTake(Player entity, ItemStack stack) {
				super.onTake(entity, stack);
				slotChanged(11, 1, 0);
			}

			@Override
			public void onQuickCraft(ItemStack a, ItemStack b) {
				super.onQuickCraft(a, b);
				slotChanged(11, 2, b.getCount() - a.getCount());
			}

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		this.customSlots.put(12, this.addSlot(new SlotItemHandler(internal, 12, 62, -23) {
			private final int slot = 12;

			@Override
			public boolean mayPickup(Player entity) {
				return false;
			}

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		this.customSlots.put(13, this.addSlot(new SlotItemHandler(internal, 13, 96, -23) {
			private final int slot = 13;

			@Override
			public boolean mayPickup(Player entity) {
				return false;
			}

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		this.customSlots.put(14, this.addSlot(new SlotItemHandler(internal, 14, 130, -23) {
			private final int slot = 14;

			@Override
			public boolean mayPickup(Player entity) {
				return false;
			}

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		// Slot 15 : Affichage XP (positionné en haut à droite, par exemple x=180, y=-23)
		this.customSlots.put(15, this.addSlot(new SlotItemHandler(internal, 15, 164, -23) {
			private final int slot = 15;

			@Override
			public boolean mayPickup(Player entity) {
				return false;
			}

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		
		for (int si = 0; si < 3; ++si)
			for (int sj = 0; sj < 9; ++sj)
				this.addSlot(new Slot(inv, sj + (si + 1) * 9, 12 + 8 + sj * 18, 0 + 84 + si * 18));
		for (int si = 0; si < 9; ++si)
			this.addSlot(new Slot(inv, si, 12 + 8 + si * 18, 0 + 142));
	}

	@Override
	public boolean stillValid(Player player) {
		if (this.bound) {
			if (this.boundItemMatcher != null)
				return this.boundItemMatcher.get();
			else if (this.boundBlockEntity != null)
				return AbstractContainerMenu.stillValid(this.access, player, this.boundBlockEntity.getBlockState().getBlock());
			else if (this.boundEntity != null)
				return this.boundEntity.isAlive();
		}
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = (Slot) this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();
			if (index < 16) { // Mise à jour de la taille (15 -> 16)
				if (!this.moveItemStackTo(itemstack1, 16, this.slots.size(), true))
					return ItemStack.EMPTY;
				slot.onQuickCraft(itemstack1, itemstack);
			} else if (!this.moveItemStackTo(itemstack1, 0, 16, false)) {
				if (index < 16 + 27) {
					if (!this.moveItemStackTo(itemstack1, 16 + 27, this.slots.size(), true))
						return ItemStack.EMPTY;
				} else {
					if (!this.moveItemStackTo(itemstack1, 16, 16 + 27, false))
						return ItemStack.EMPTY;
				}
				return ItemStack.EMPTY;
			}
			if (itemstack1.getCount() == 0)
				slot.set(ItemStack.EMPTY);
			else
				slot.setChanged();
			if (itemstack1.getCount() == itemstack.getCount())
				return ItemStack.EMPTY;
			slot.onTake(playerIn, itemstack1);
		}
		return itemstack;
	}

	@Override
	protected boolean moveItemStackTo(ItemStack p_38904_, int p_38905_, int p_38906_, boolean p_38907_) {
		boolean flag = false;
		int i = p_38905_;
		if (p_38907_) {
			i = p_38906_ - 1;
		}
		if (p_38904_.isStackable()) {
			while (!p_38904_.isEmpty()) {
				if (p_38907_) {
					if (i < p_38905_) {
						break;
					}
				} else if (i >= p_38906_) {
					break;
				}
				Slot slot = this.slots.get(i);
				ItemStack itemstack = slot.getItem();
				if (slot.mayPlace(itemstack) && !itemstack.isEmpty() && ItemStack.isSameItemSameTags(p_38904_, itemstack)) {
					int j = itemstack.getCount() + p_38904_.getCount();
					int maxSize = Math.min(slot.getMaxStackSize(), p_38904_.getMaxStackSize());
					if (j <= maxSize) {
						p_38904_.setCount(0);
						itemstack.setCount(j);
						slot.set(itemstack);
						flag = true;
					} else if (itemstack.getCount() < maxSize) {
						p_38904_.shrink(maxSize - itemstack.getCount());
						itemstack.setCount(maxSize);
						slot.set(itemstack);
						flag = true;
					}
				}
				if (p_38907_) {
					--i;
				} else {
					++i;
				}
			}
		}
		if (!p_38904_.isEmpty()) {
			if (p_38907_) {
				i = p_38906_ - 1;
			} else {
				i = p_38905_;
			}
			while (true) {
				if (p_38907_) {
					if (i < p_38905_) {
						break;
					}
				} else if (i >= p_38906_) {
					break;
				}
				Slot slot1 = this.slots.get(i);
				ItemStack itemstack1 = slot1.getItem();
				if (itemstack1.isEmpty() && slot1.mayPlace(p_38904_)) {
					if (p_38904_.getCount() > slot1.getMaxStackSize()) {
						slot1.setByPlayer(p_38904_.split(slot1.getMaxStackSize()));
					} else {
						slot1.setByPlayer(p_38904_.split(p_38904_.getCount()));
					}
					slot1.setChanged();
					flag = true;
					break;
				}
				if (p_38907_) {
					--i;
				} else {
					++i;
				}
			}
		}
		return flag;
	}

	@Override
	public void removed(Player playerIn) {
        super.removed(playerIn);
        if(!bound&&playerIn instanceof ServerPlayer serverPlayer){
            if(serverPlayer.isAlive()&&!serverPlayer.hasDisconnected()){
                for(int slot=0;slot<internal.getSlots();slot++)if(isRealSlot(slot)){
                    ItemStack stack=internal.getStackInSlot(slot).copy();serverPlayer.getInventory().add(stack);
                    ((ItemStackHandler)internal).setStackInSlot(slot,stack);
                }
            }
            persistEscrow();
            fr.renblood.npcshopkeeper.manager.integration.TradeEscrowRecovery.persist(serverPlayer);
        }
    }

	private void slotChanged(int slotid, int ctype, int meta) {
		if (this.world != null && this.world.isClientSide()) {
			Npcshopkeeper.PACKET_HANDLER.sendToServer(new TradeSlotMessage(slotid, x, y, z, ctype, meta));
			TradeSlotMessage.handleSlotAction(entity, slotid, ctype, meta, x, y, z);
		}
	}

	public Map<Integer, Slot> get() {
		return customSlots;
	}
}
