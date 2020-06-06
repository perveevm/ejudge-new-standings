package ru.strategy48.ejudge.contest;

public enum Status {
    OK, // OK
    CE, // Compilation error
    RT, // Runtime error
    TL, // Time limit exceeded
    PE, // Presentation error
    WA, // Wrong answer
    CF, // Check failed
    PT, // Partial solution
    AC, // Accepted
    IG, // Ignored
    DQ, // Disqualified
    PD, // Pending
    ML, // Memory limit exceeded
    SE, // Security violation
    SV, // Code style violation
    WT, // Wall time limit exceeded
    PR, // Pending review
    RJ, // Rejected or Rejudge
    SK, // Skipped
    SY, // Synchronization error
    SM, // Summoned for defence
    RU, // Running
    CD, // Compiled
    CG, // Compiling
    AV, // Available for testing
    EM, // Empty
    VS, // Virtual start
    VT  // Virtual finish
}
