package com.fa26se088.filmlink.service.lab.impl;

import com.fa26se088.filmlink.entity.lab.FilmLab;
import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import com.fa26se088.filmlink.repository.lab.FilmLabRepository;
import com.fa26se088.filmlink.service.lab.LabAccessService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabAccessServiceImpl implements LabAccessService {

  private final FilmLabRepository filmLabRepository;

  @Override
  @Transactional(readOnly = true)
  public FilmLab requireOwnedLab(Long ownerAccountId) {
    return filmLabRepository
        .findByOwnerAccountId(ownerAccountId)
        .orElseThrow(LabAccessServiceImpl::labNotFound);
  }

  /**
   * CR-14: lab tồn tại nhưng thuộc chủ khác trả 403, không giả làm 404, để FE phân biệt được "sai
   * đường dẫn" với "không có quyền".
   */
  @Override
  @Transactional(readOnly = true)
  public FilmLab requireOwnedLab(Long ownerAccountId, Long labId) {
    FilmLab lab = filmLabRepository.findById(labId).orElseThrow(LabAccessServiceImpl::labNotFound);
    if (!Objects.equals(lab.getOwnerAccountId(), ownerAccountId)) {
      throw new BusinessException(
          ErrorCode.LAB_ACCESS_DENIED, "Bạn không có quyền thao tác trên Film Lab này.");
    }
    return lab;
  }

  private static BusinessException labNotFound() {
    return new BusinessException(ErrorCode.LAB_NOT_FOUND, "Không tìm thấy Film Lab.");
  }
}
