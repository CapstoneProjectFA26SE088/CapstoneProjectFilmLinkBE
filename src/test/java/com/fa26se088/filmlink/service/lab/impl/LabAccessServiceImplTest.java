package com.fa26se088.filmlink.service.lab.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fa26se088.filmlink.entity.lab.FilmLab;
import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import com.fa26se088.filmlink.repository.lab.FilmLabRepository;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LabAccessServiceImplTest {

  private static final Long OWNER_ID = 7L;
  private static final Long OTHER_OWNER_ID = 8L;
  private static final Long LAB_ID = 100L;

  @Mock private FilmLabRepository filmLabRepository;

  private LabAccessServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new LabAccessServiceImpl(filmLabRepository);
  }

  // --- requireOwnedLab(ownerAccountId) ---

  @Test
  void requireOwnedLab_ownerHasLab_returnsIt() {
    FilmLab lab = lab(LAB_ID, OWNER_ID);
    when(filmLabRepository.findByOwnerAccountId(OWNER_ID)).thenReturn(Optional.of(lab));

    assertThat(service.requireOwnedLab(OWNER_ID)).isSameAs(lab);
  }

  @Test
  void requireOwnedLab_ownerHasNoLab_returnsLabNotFound() {
    when(filmLabRepository.findByOwnerAccountId(OWNER_ID)).thenReturn(Optional.empty());

    assertError(() -> service.requireOwnedLab(OWNER_ID), ErrorCode.LAB_NOT_FOUND);
  }

  // --- requireOwnedLab(ownerAccountId, labId) ---

  @Test
  void requireOwnedLabById_ownLab_returnsIt() {
    FilmLab lab = lab(LAB_ID, OWNER_ID);
    when(filmLabRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

    assertThat(service.requireOwnedLab(OWNER_ID, LAB_ID)).isSameAs(lab);
  }

  @Test
  void requireOwnedLabById_labMissing_returnsLabNotFound() {
    when(filmLabRepository.findById(LAB_ID)).thenReturn(Optional.empty());

    assertError(() -> service.requireOwnedLab(OWNER_ID, LAB_ID), ErrorCode.LAB_NOT_FOUND);
  }

  /** CR-14: lab của chủ khác trả 403, không trả lab. */
  @Test
  void requireOwnedLabById_otherOwnersLab_returnsAccessDenied() {
    when(filmLabRepository.findById(LAB_ID)).thenReturn(Optional.of(lab(LAB_ID, OTHER_OWNER_ID)));

    assertError(() -> service.requireOwnedLab(OWNER_ID, LAB_ID), ErrorCode.LAB_ACCESS_DENIED);
  }

  private static void assertError(ThrowingCallable call, ErrorCode expected) {
    assertThatThrownBy(call)
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(expected);
  }

  private static FilmLab lab(Long id, Long ownerAccountId) {
    FilmLab lab = new FilmLab();
    ReflectionTestUtils.setField(lab, "id", id);
    lab.setOwnerAccountId(ownerAccountId);
    return lab;
  }
}
