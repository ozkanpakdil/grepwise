import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { NotificationChannel } from '@/api/alarm';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';

interface NotificationPreferencesProps {
  channels: NotificationChannel[];
  onChange: (channels: NotificationChannel[]) => void;
}

export default function NotificationPreferences({ channels, onChange }: NotificationPreferencesProps) {
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [editingIndex, setEditingIndex] = useState<number | null>(null);

  const [newChannel, setNewChannel] = useState<NotificationChannel>({
    type: 'EMAIL',
    destination: '',
    config: {},
  });

  const handleAddChannel = () => {
    if (!newChannel.destination.trim()) {
      return; // Don't add empty destinations
    }

    if (editingIndex !== null) {
      // Edit existing channel
      const updatedChannels = [...channels];
      updatedChannels[editingIndex] = newChannel;
      onChange(updatedChannels);
    } else {
      // Add new channel
      onChange([...channels, newChannel]);
    }

    // Reset form and close dialog
    setNewChannel({ type: 'EMAIL', destination: '', config: {} });
    setIsAddDialogOpen(false);
    setEditingIndex(null);
  };

  const handleEditChannel = (index: number) => {
    setNewChannel({ ...channels[index] });
    setEditingIndex(index);
    setIsAddDialogOpen(true);
  };

  const handleRemoveChannel = (index: number) => {
    const updatedChannels = [...channels];
    updatedChannels.splice(index, 1);
    onChange(updatedChannels);
  };

  const getChannelTypeLabel = (type: string) => {
    switch (type.toUpperCase()) {
      case 'EMAIL':
        return 'Email';
      case 'SLACK':
        return 'Slack';
      case 'WEBHOOK':
        return 'Webhook';
      default:
        return type;
    }
  };

  const getDestinationPlaceholder = (type: string) => {
    switch (type.toUpperCase()) {
      case 'EMAIL':
        return 'admin@example.com';
      case 'SLACK':
        return '#channel or webhook URL';
      case 'WEBHOOK':
        return 'https://example.com/webhook';
      default:
        return '';
    }
  };

  const getDestinationLabel = (type: string) => {
    switch (type.toUpperCase()) {
      case 'EMAIL':
        return 'Email Address';
      case 'SLACK':
        return 'Slack Channel/Webhook';
      case 'WEBHOOK':
        return 'Webhook URL';
      default:
        return 'Destination';
    }
  };

  const renderConfigFields = () => {
    switch (newChannel.type.toUpperCase()) {
      case 'EMAIL':
        return (
          <div className="space-y-2">
            <Label htmlFor="subject">Email Subject</Label>
            <Input
              id="subject"
              value={newChannel.config?.subject || ''}
              onChange={(e) =>
                setNewChannel({
                  ...newChannel,
                  config: { ...newChannel.config, subject: e.target.value },
                })
              }
              placeholder="Alarm Notification from GrepWise"
            />

            <Label htmlFor="senderName">Sender Name (optional)</Label>
            <Input
              id="senderName"
              value={newChannel.config?.senderName || ''}
              onChange={(e) =>
                setNewChannel({
                  ...newChannel,
                  config: { ...newChannel.config, senderName: e.target.value },
                })
              }
              placeholder="GrepWise Notifications"
            />

            <Label htmlFor="messageTemplate">Message Template</Label>
            <Textarea
              id="messageTemplate"
              value={newChannel.config?.messageTemplate || ''}
              onChange={(e) =>
                setNewChannel({
                  ...newChannel,
                  config: { ...newChannel.config, messageTemplate: e.target.value },
                })
              }
              placeholder="Alarm {alarm_name} was triggered at {timestamp}. Details: {details}"
              rows={4}
            />
          </div>
        );
      case 'SLACK':
        return (
          <div className="space-y-2">
            <Label htmlFor="username">Username (optional)</Label>
            <Input
              id="username"
              value={newChannel.config?.username || ''}
              onChange={(e) =>
                setNewChannel({
                  ...newChannel,
                  config: { ...newChannel.config, username: e.target.value },
                })
              }
              placeholder="GrepWise Bot"
            />
          </div>
        );
      case 'WEBHOOK':
        return (
          <div className="space-y-2">
            <Label htmlFor="method">HTTP Method</Label>
            <Select
              value={newChannel.config?.method || 'POST'}
              onValueChange={(value) =>
                setNewChannel({
                  ...newChannel,
                  config: { ...newChannel.config, method: value },
                })
              }
            >
              <SelectTrigger id="method">
                <SelectValue placeholder="Select method" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="POST">POST</SelectItem>
                <SelectItem value="PUT">PUT</SelectItem>
              </SelectContent>
            </Select>

            <Label htmlFor="headers">Headers (JSON)</Label>
            <Textarea
              id="headers"
              value={newChannel.config?.headers || ''}
              onChange={(e) =>
                setNewChannel({
                  ...newChannel,
                  config: { ...newChannel.config, headers: e.target.value },
                })
              }
              placeholder='{"Content-Type": "application/json"}'
              rows={3}
            />
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h3 className="text-sm font-medium">Notification Channels</h3>
        <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
          <DialogTrigger asChild>
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setNewChannel({ type: 'EMAIL', destination: '', config: {} });
                setEditingIndex(null);
              }}
            >
              Add Channel
            </Button>
          </DialogTrigger>
          <DialogContent className="sm:max-w-[425px]">
            <DialogHeader>
              <DialogTitle>{editingIndex !== null ? 'Edit' : 'Add'} Notification Channel</DialogTitle>
              <DialogDescription>
                Configure where and how notifications will be sent when this alarm is triggered.
              </DialogDescription>
            </DialogHeader>
            <div className="grid gap-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="type">Channel Type</Label>
                <Select
                  value={newChannel.type}
                  onValueChange={(value) =>
                    setNewChannel({
                      ...newChannel,
                      type: value,
                      config: {}, // Reset config when changing type
                    })
                  }
                >
                  <SelectTrigger id="type">
                    <SelectValue placeholder="Select type" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="EMAIL">Email</SelectItem>
                    <SelectItem value="SLACK">Slack</SelectItem>
                    <SelectItem value="WEBHOOK">Webhook</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="destination">{getDestinationLabel(newChannel.type)}</Label>
                <Input
                  id="destination"
                  value={newChannel.destination}
                  onChange={(e) =>
                    setNewChannel({
                      ...newChannel,
                      destination: e.target.value,
                    })
                  }
                  placeholder={getDestinationPlaceholder(newChannel.type)}
                />
              </div>

              {renderConfigFields()}
            </div>
            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => {
                  setIsAddDialogOpen(false);
                  setEditingIndex(null);
                }}
              >
                Cancel
              </Button>
              <Button onClick={handleAddChannel}>{editingIndex !== null ? 'Save' : 'Add'}</Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      {channels.length === 0 ? (
        <div className="text-center py-4 border rounded-md bg-muted/20">
          <p className="text-sm text-muted-foreground">No notification channels configured</p>
          <p className="text-xs text-muted-foreground mt-1">
            Add channels to receive notifications when this alarm is triggered
          </p>
        </div>
      ) : (
        <div className="space-y-2">
          {channels.map((channel, index) => (
            <div key={index} className="flex items-center justify-between p-3 border rounded-md bg-background">
              <div>
                <div className="flex items-center space-x-2">
                  <span className="font-medium text-sm">{getChannelTypeLabel(channel.type)}</span>
                  <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary">{channel.type}</span>
                </div>
                <div className="text-sm text-muted-foreground mt-1">{channel.destination}</div>
              </div>
              <div className="flex space-x-2">
                <Button variant="ghost" size="sm" onClick={() => handleEditChannel(index)}>
                  Edit
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => handleRemoveChannel(index)}
                  className="text-destructive hover:text-destructive"
                >
                  Remove
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
