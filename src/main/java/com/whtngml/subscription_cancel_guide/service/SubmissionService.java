package com.whtngml.subscription_cancel_guide.service;

import com.whtngml.subscription_cancel_guide.domain.Submission;
import com.whtngml.subscription_cancel_guide.dto.SubmissionForm;
import com.whtngml.subscription_cancel_guide.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {

    private final SubmissionRepository submissionRepository;

    @Transactional
    public Long submit(SubmissionForm form) {
        Submission submission = new Submission(form.getServiceName().strip(), form.getRawText().strip());
        return submissionRepository.save(submission).getId();
    }
}
