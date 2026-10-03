package com.referralcrm.service;

import com.referralcrm.api.ApiException;
import com.referralcrm.domain.*;
import com.referralcrm.repository.ContactRepository;
import com.referralcrm.repository.GeneratedMessageRepository;
import com.referralcrm.repository.OutreachRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageWorkflowService {
    private final GeneratedMessageRepository messages; private final ContactRepository contacts; private final OutreachRepository outreach;
    public MessageWorkflowService(GeneratedMessageRepository messages,ContactRepository contacts,OutreachRepository outreach) {
        this.messages=messages; this.contacts=contacts; this.outreach=outreach;
    }
    private GeneratedMessage owned(UUID id,UUID userId) { return messages.findByIdAndUserIdAndDeletedAtIsNull(id,userId).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Message draft not found")); }

    @Transactional
    public GeneratedMessage edit(UUID userId,UUID id,String text) {
        GeneratedMessage old=owned(id,userId);
        if("SENT".equals(old.getStatus())) throw new ApiException(HttpStatus.CONFLICT,"A sent message cannot be edited");
        GeneratedMessage copy=new GeneratedMessage(); copy.setUserId(userId); copy.setContactId(old.getContactId()); copy.setCompanyId(old.getCompanyId()); copy.setResumeId(old.getResumeId());
        copy.setRole(old.getRole()); copy.setVariant(old.getVariant()); copy.setVersion(messages.findFirstByContactIdAndVariantAndDeletedAtIsNullOrderByVersionDesc(old.getContactId(),old.getVariant()).map(m->m.getVersion()+1).orElse(old.getVersion()+1));
        copy.setChannel(old.getChannel()); copy.setMessageText(text.trim()); copy.setRecommendationScore(old.getRecommendationScore()); copy.setRecommendationReason(old.getRecommendationReason()); copy.setResumeReason(old.getResumeReason()); copy.setStatus("DRAFT");
        return messages.save(copy);
    }

    @Transactional
    public GeneratedMessage approve(UUID userId,UUID id) {
        GeneratedMessage m=owned(id,userId); if("SENT".equals(m.getStatus())) throw new ApiException(HttpStatus.CONFLICT,"This message has already been marked sent");
        m.setStatus("APPROVED"); m.setApprovedAt(OffsetDateTime.now()); return messages.save(m);
    }

    @Transactional
    public GeneratedMessage markSent(UUID userId,UUID id,String channel) {
        GeneratedMessage m=owned(id,userId);
        if(!"APPROVED".equals(m.getStatus())) throw new ApiException(HttpStatus.CONFLICT,"Approve this draft before marking it sent");
        Contact c=contacts.findById(m.getContactId()).filter(x->x.getDeletedAt()==null&&x.getUserId().equals(userId)).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Contact not found"));
        OffsetDateTime now=OffsetDateTime.now(); Outreach entry=new Outreach(); entry.setUserId(userId); entry.setContactId(c.getId()); entry.setResumeId(m.getResumeId()); entry.setGeneratedMessageId(m.getId());
        entry.setChannel(channel); entry.setMessageVersion(m.getVariant()+"_v"+m.getVersion()); entry.setMessageText(m.getMessageText()); entry.setSentAt(now); outreach.save(entry);
        m.setChannel(channel); m.setSentAt(now); m.setStatus("SENT"); messages.save(m);
        if(c.getStatus()==ContactStatus.NOT_CONTACTED||c.getStatus()==ContactStatus.MESSAGE_READY) { c.setStatus(ContactStatus.CONTACTED); contacts.save(c); }
        return m;
    }
}
