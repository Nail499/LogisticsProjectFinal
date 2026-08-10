package com.ltc.logisticsproject.entity;

// CustomsDeclaration-ın həyat dövrü: DRAFT (rüsum hesablanıb, hələ
// göndərilməyib) -> SUBMITTED (bəyan edilib, gömrükdə baxılır) -> CLEARED
// (buraxılış verilib) və ya REJECTED (rədd edilib, düzəliş tələb olunur).
public enum DeclarationStatus {
    DRAFT, SUBMITTED, CLEARED, REJECTED
}
